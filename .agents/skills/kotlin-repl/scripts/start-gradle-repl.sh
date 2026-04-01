#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF' >&2
Usage: start-gradle-repl.sh [--module <module>] [options]

Compile a Gradle JVM target, resolve its runtime jars, assemble a kotlinc-ready classpath, and launch the REPL.

Options:
  --module <module>            Gradle module or project path, for example domain or :domain.
                               Defaults to $MODULE or the nearest Gradle module above the current directory.
  --kind <jvm|kmp-jvm>         Defaults to jvm.
  --build-task <task>          Override the compile/build task. Accepts a task name or full Gradle task path.
  --configuration <name>       Override the dependency configuration to resolve.
  --project-dir <dir>          Project root that contains ./gradlew. Defaults to the nearest parent with ./gradlew.
  --state-dir <dir>            REPL state directory. Defaults to <project-dir>/build/codex-repl/<module>.
  --kotlinc <path>             kotlinc binary to launch. Defaults to kotlinc on PATH.
  --classpath-entry <path>     Extra classpath entry to append. May be repeated.
  --list-configurations        Print resolvable configuration names for the module and exit.
  --print-classpath            Print the assembled classpath and exit.
  --print-entries              Print one resolved classpath entry per line and exit.
  -h, --help                   Show this help.
EOF
}

normalize_path() {
    local path="$1"
    (
        cd "$path"
        pwd
    )
}

absolute_existing_path() {
    local path="$1"
    if [[ -d "$path" ]]; then
        normalize_path "$path"
        return
    fi

    local dir
    local base
    dir=$(dirname "$path")
    base=$(basename "$path")
    printf '%s/%s\n' "$(normalize_path "$dir")" "$base"
}

find_gradle_project_dir() {
    local start_dir="$1"
    local current_dir
    current_dir=$(absolute_existing_path "$start_dir")

    while true; do
        if [[ -x "$current_dir/gradlew" ]]; then
            printf '%s\n' "$current_dir"
            return 0
        fi

        if [[ "$current_dir" == "/" ]]; then
            return 1
        fi

        current_dir=$(dirname "$current_dir")
    done
}

normalize_module_path() {
    local module="$1"
    if [[ "$module" == ":" ]]; then
        printf ':\n'
    elif [[ "$module" == :* ]]; then
        printf '%s\n' "$module"
    else
        printf ':%s\n' "$module"
    fi
}

module_dir_from_path() {
    local module_path="$1"
    if [[ "$module_path" == ":" ]]; then
        printf '.\n'
    else
        printf '%s\n' "${module_path#:}" | tr ':' '/'
    fi
}

module_slug_from_path() {
    local module_path="$1"
    local slug="${module_path#:}"
    if [[ -z "$slug" ]]; then
        printf 'root\n'
        return
    fi
    printf '%s\n' "${slug//:/-}"
}

task_path_for_module() {
    local module_path="$1"
    local task_name="$2"
    if [[ "$task_name" == :* ]]; then
        printf '%s\n' "$task_name"
    elif [[ "$module_path" == ":" ]]; then
        printf ':%s\n' "$task_name"
    else
        printf '%s:%s\n' "$module_path" "$task_name"
    fi
}

detect_module_from_workdir() {
    local project_dir="$1"
    local work_dir="$2"
    local current_dir
    current_dir=$(absolute_existing_path "$work_dir")

    while [[ "$current_dir" == "$project_dir" || "$current_dir" == "$project_dir/"* ]]; do
        if [[ "$current_dir" != "$project_dir" ]] && [[ -f "$current_dir/build.gradle.kts" || -f "$current_dir/build.gradle" ]]; then
            local relative_path="${current_dir#$project_dir/}"
            printf ':%s\n' "${relative_path//\//:}"
            return 0
        fi

        if [[ "$current_dir" == "$project_dir" ]]; then
            break
        fi

        current_dir=$(dirname "$current_dir")
    done

    return 1
}

suggest_modules() {
    local project_dir="$1"
    find "$project_dir" -mindepth 1 \( -name build.gradle -o -name build.gradle.kts \) -print \
        | sed -e "s|$project_dir/||" -e 's|/build.gradle\.kts$||' -e 's|/build.gradle$||' \
        | sed -e 's|/|:|g' -e 's|^|:|' \
        | sort -u
}

append_unique() {
    local value="$1"
    if [[ ${classpath_entries+x} == x ]]; then
        local existing_count="${#classpath_entries[@]}"
        if ((existing_count > 0)); then
            local existing
            for existing in "${classpath_entries[@]}"; do
                [[ "$existing" == "$value" ]] && return 0
            done
        fi
    fi
    classpath_entries+=("$value")
}

collect_stdlib_versions() {
    printf '%s\n' "${classpath_entries[@]}" \
        | sed -nE \
            -e 's|.*/kotlin-stdlib-jdk7-([0-9][^/]*)\.jar$|\1|p' \
            -e 's|.*/kotlin-stdlib-jdk8-([0-9][^/]*)\.jar$|\1|p' \
            -e 's|.*/kotlin-stdlib-([0-9][^/]*)\.jar$|\1|p' \
        | sort -u
}

detect_kotlinc_version() {
    local version_output
    version_output=$("$kotlinc_bin" -version 2>&1 || true)
    printf '%s\n' "$version_output" | sed -nE 's/.*kotlinc(-jvm)? ([0-9]+\.[0-9]+(\.[0-9]+)?).*/\2/p' | head -n 1
}

module="${MODULE:-}"
kind="jvm"
build_task=""
configuration=""
project_dir="${PROJECT_DIR:-}"
state_dir=""
kotlinc_bin="kotlinc"
mode="launch"
extra_classpath_entries=()
extra_classpath_entries_count=0

while (($# > 0)); do
    case "$1" in
        --module)
            [[ $# -ge 2 ]] || { usage; exit 64; }
            module="$2"
            shift 2
            ;;
        --kind)
            [[ $# -ge 2 ]] || { usage; exit 64; }
            kind="$2"
            shift 2
            ;;
        --build-task)
            [[ $# -ge 2 ]] || { usage; exit 64; }
            build_task="$2"
            shift 2
            ;;
        --configuration)
            [[ $# -ge 2 ]] || { usage; exit 64; }
            configuration="$2"
            shift 2
            ;;
        --project-dir)
            [[ $# -ge 2 ]] || { usage; exit 64; }
            project_dir="$2"
            shift 2
            ;;
        --state-dir)
            [[ $# -ge 2 ]] || { usage; exit 64; }
            state_dir="$2"
            shift 2
            ;;
        --kotlinc)
            [[ $# -ge 2 ]] || { usage; exit 64; }
            kotlinc_bin="$2"
            shift 2
            ;;
        --classpath-entry)
            [[ $# -ge 2 ]] || { usage; exit 64; }
            extra_classpath_entries+=("$2")
            extra_classpath_entries_count=$((extra_classpath_entries_count + 1))
            shift 2
            ;;
        --list-configurations)
            mode="list-configurations"
            shift
            ;;
        --print-classpath)
            mode="print-classpath"
            shift
            ;;
        --print-entries)
            mode="print-entries"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage
            exit 64
            ;;
    esac
done

if [[ -z "$project_dir" ]]; then
    if ! project_dir=$(find_gradle_project_dir "$PWD"); then
        echo "Could not find an executable ./gradlew from $PWD or its parents." >&2
        exit 1
    fi
else
    project_dir=$(absolute_existing_path "$project_dir")
fi

case "$kind" in
    jvm|kmp-jvm) ;;
    *)
        echo "Unsupported --kind value: $kind" >&2
        usage
        exit 64
        ;;
esac

if [[ -z "$module" ]]; then
    module=$(detect_module_from_workdir "$project_dir" "$PWD" || true)
fi

if [[ -z "$module" ]]; then
    echo "Missing --module argument." >&2
    echo "Run this from inside a Gradle module directory, or set MODULE=<module>." >&2
    module_hints=$(suggest_modules "$project_dir" || true)
    if [[ -n "$module_hints" ]]; then
        echo "Detected modules:" >&2
        while IFS= read -r module_hint; do
            [[ -n "$module_hint" ]] || continue
            printf '  %s\n' "$module_hint" >&2
        done <<< "$module_hints"
    fi
    usage
    exit 64
fi

module_path=$(normalize_module_path "$module")
module_dir=$(module_dir_from_path "$module_path")
module_slug=$(module_slug_from_path "$module_path")

if [[ -z "$state_dir" ]]; then
    state_dir="$project_dir/build/codex-repl/$module_slug"
fi
mkdir -p "$(dirname "$state_dir")"
state_dir=$(absolute_existing_path "$(dirname "$state_dir")")/$(basename "$state_dir")

case "$kind" in
    jvm)
        build_task=${build_task:-classes}
        configuration=${configuration:-runtimeClasspath}
        output_candidates=(
            "$project_dir/$module_dir/build/classes/kotlin/main"
            "$project_dir/$module_dir/build/classes/java/main"
            "$project_dir/$module_dir/build/resources/main"
        )
        ;;
    kmp-jvm)
        build_task=${build_task:-compileKotlinJvm}
        configuration=${configuration:-jvmRuntimeClasspath}
        output_candidates=(
            "$project_dir/$module_dir/build/classes/kotlin/jvm/main"
            "$project_dir/$module_dir/build/classes/java/jvm/main"
            "$project_dir/$module_dir/build/processedResources/jvm/main"
            "$project_dir/$module_dir/build/resources/jvm/main"
        )
        ;;
esac

build_task=$(task_path_for_module "$module_path" "$build_task")
script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
init_script="$script_dir/gradle-classpath.init.gradle.kts"
gradle_helper_task=$(task_path_for_module "$module_path" "codexPrintRuntimeInfo")
gradle_list_task=$(task_path_for_module "$module_path" "codexListResolvableConfigurations")

if [[ ! -x "$project_dir/gradlew" ]]; then
    echo "Could not find an executable ./gradlew under $project_dir" >&2
    exit 1
fi

if [[ "$mode" == "list-configurations" ]]; then
    (
        cd "$project_dir"
        ./gradlew --no-configuration-cache -q -I "$init_script" "$gradle_list_task"
    )
    exit 0
fi

echo "Compiling $module_path with $build_task" >&2
(
    cd "$project_dir"
    ./gradlew "$build_task"
)

echo "Resolving $configuration for $module_path" >&2
dependency_entries=()
resolved_output_entries=()
while IFS= read -r entry; do
    case "$entry" in
        OUTPUT$'\t'*)
            resolved_output_entries+=("${entry#*$'\t'}")
            ;;
        DEPENDENCY$'\t'*)
            dependency_entries+=("${entry#*$'\t'}")
            ;;
        *)
            dependency_entries+=("$entry")
            ;;
    esac
done < <(
    cd "$project_dir"
    ./gradlew --no-configuration-cache -q -I "$init_script" "$gradle_helper_task" -PcodexConfigurationName="$configuration"
)

classpath_entries=()
if ((${#resolved_output_entries[@]} > 0)); then
    for output_entry in "${resolved_output_entries[@]}"; do
        append_unique "$(absolute_existing_path "$output_entry")"
    done
fi

for candidate in "${output_candidates[@]}"; do
    if [[ -e "$candidate" ]]; then
        append_unique "$(absolute_existing_path "$candidate")"
    fi
done

if ((extra_classpath_entries_count > 0)); then
    for extra_entry in "${extra_classpath_entries[@]}"; do
        if [[ -e "$extra_entry" ]]; then
            append_unique "$(absolute_existing_path "$extra_entry")"
        else
            echo "Classpath entry does not exist: $extra_entry" >&2
            exit 1
        fi
    done
fi

if ((${#classpath_entries[@]} == 0)); then
    echo "Could not find compiled output directories for $module_path." >&2
    echo "Checked:" >&2
    printf '  %s\n' "${output_candidates[@]}" >&2
    echo "Try --kind kmp-jvm, or override with --classpath-entry <path>." >&2
    exit 1
fi

if ((${#dependency_entries[@]} > 0)); then
    for entry in "${dependency_entries[@]}"; do
        append_unique "$entry"
    done
fi

classpath=$(printf '%s:' "${classpath_entries[@]}")
classpath="${classpath%:}"

stdlib_versions=$(collect_stdlib_versions || true)
kotlinc_version=$(detect_kotlinc_version || true)
stdlib_version_count=$(printf '%s\n' "$stdlib_versions" | sed '/^$/d' | wc -l | tr -d ' ')
stdlib_versions_csv=$(printf '%s\n' "$stdlib_versions" | sed '/^$/d' | awk 'NR == 1 { printf "%s", $0; next } { printf ", %s", $0 }')
if [[ "$stdlib_version_count" -gt 1 ]]; then
    echo "Warning: resolved classpath mixes Kotlin stdlib versions: $stdlib_versions_csv." >&2
    echo "Use --kotlinc <path> with the intended version, or narrow the classpath before relying on REPL results." >&2
elif [[ "$stdlib_version_count" -eq 1 ]]; then
    stdlib_version=$(printf '%s\n' "$stdlib_versions" | sed -n '1p')
    if [[ -n "$kotlinc_version" && "$stdlib_version" != "$kotlinc_version" ]]; then
        echo "Warning: kotlinc $kotlinc_version does not match kotlin-stdlib $stdlib_version on the resolved classpath." >&2
        echo "Use --kotlinc <path> to point at a matching compiler if REPL startup or evaluation fails." >&2
    fi
fi

case "$mode" in
    print-classpath)
        printf '%s\n' "$classpath"
        exit 0
        ;;
    print-entries)
        printf '%s\n' "${classpath_entries[@]}"
        exit 0
        ;;
esac

exec "$script_dir/start-repl.sh" --classpath "$classpath" --state-dir "$state_dir" --kotlinc "$kotlinc_bin"
