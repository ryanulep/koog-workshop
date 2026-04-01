#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF' >&2
Usage: start-repl.sh --classpath <classpath> [--state-dir <dir>] [--kotlinc <path>] [-- <extra kotlinc args>]

Launch kotlinc -Xrepl with REPL state stored inside the current project instead of the user's home directory.
EOF
}

classpath=""
state_dir="$PWD/build/codex-repl"
kotlinc_bin="kotlinc"
extra_args=()

while (($# > 0)); do
    case "$1" in
        --classpath)
            [[ $# -ge 2 ]] || { usage; exit 64; }
            classpath="$2"
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
        --)
            shift
            extra_args=("$@")
            break
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

if [[ -z "$classpath" ]]; then
    echo "Missing required --classpath argument." >&2
    usage
    exit 64
fi

mkdir -p "$state_dir"

cmd=("$kotlinc_bin" -Xrepl -J-Duser.home="$state_dir" -classpath "$classpath")
if ((${#extra_args[@]} > 0)); then
    cmd+=("${extra_args[@]}")
fi

HOME="$state_dir" exec "${cmd[@]}"
