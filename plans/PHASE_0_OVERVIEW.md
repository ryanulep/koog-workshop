# Fantasy Ecommerce — Design Overview

## Context

Fully immersive in-world fantasy merchant shop (D&D/WoW/BG themed). Kotlin Compose Multiplatform desktop app. Characters buy potions, weapons, armor from named merchants using fantasy currencies.

## Design Decisions

| Decision | Choice |
|---|---|
| Store framing | In-world fantasy merchant shop |
| Account model | One account = one character, no RPG attribute gating |
| Wallet | Persistent, ledger-based (balance = SUM of transactions) |
| Currencies | Multiple independent (Gold, Crowns, etc.) with fixed conversion rates |
| Products | 5 categories (Weapons, Armor, Potions, Scrolls, Misc), rarity tiers, finite stock |
| Product attributes | Sealed class hierarchy in Kotlin, single table with nullable columns in DB |
| Pricing | Each product priced in exactly one currency |
| Merchants | Multiple named shops, each offering specific shipping methods |
| Cart | Global cart across merchants |
| Orders | Parent Order -> Sub-orders (one per merchant), rich lifecycle |
| Order states | Pending -> Confirmed -> Crafting -> Shipped -> Delivered + Cancelled/Refunded |
| Payment | Auto-convert currencies at checkout using fixed rates, deduct from wallet |
| Shipping | DB-configurable fantasy methods (Courier Raven, Teleportation, etc.) per merchant |
| Refunds | Back to wallet |
| Reviews | Star rating (1-5) + text, tied to purchase |
| Wishlists | Per-character saved products |
| No personal inventory | Order history only |
| DB framework | Exposed 1.1.1 (DSL + JDBC) with SQLite JDBC |

## Phases

| Phase | Name | Status | Description |
|---|---|---|---|
| 1 | Dependencies | Done | Exposed 1.1.1, SQLite JDBC, and kotlinx-datetime are in the build; `composeApp:compileKotlinJvm` passes on Java 21 |
| 2 | Characters & Currency | Done | Enums, Character/Currency domain + tables + tests |
| 3 | Merchants & Products | Done | Merchant, Product sealed class, tables + tests |
| 4 | Shipping & Cart | Done | Shipping methods, cart, wishlist domain + tables + tests |
| 5 | Orders | Done | Order/SubOrder/OrderItem hierarchy + tables + tests |
| 6 | Reviews | Next | Review system, tables + tests |
| 7 | Database Factory | Pending | Unified init, full schema smoke test |

## Key Design Notes

### Wallet balance derivation
No `wallets` table. Balance = `SELECT currency_id, SUM(amount) FROM transactions WHERE character_id = ? GROUP BY currency_id`.

### Currency exchange audit
Two `Transaction` rows per exchange sharing the same `referenceId` + `referenceType = "EXCHANGE"`:
- `EXCHANGE_DEBIT` (negative, source currency)
- `EXCHANGE_CREDIT` (positive, target currency)

### Checkout flow
1. Group cart items by merchant
2. Create parent `Order`
3. Per merchant: create `SubOrder` with shipping method
4. Per item: create `OrderItem` with snapshotted price/currency
5. Create `Transaction` PURCHASE debits
6. If conversion needed: EXCHANGE_DEBIT + EXCHANGE_CREDIT pairs
7. Decrement `products.stock`
8. Clear cart

### Money representation
All amounts as `Long` (minor units). Display layer converts to Gold/Silver/Copper.

### Enum storage
All enums stored as `varchar` (`.name`). Mapping in repository/mapper layer.

## File Organization (final state)

```
composeApp/src/jvmMain/kotlin/org/example/project/
  domain/
    enums/          (7 enum files)
    model/          (9 model files)
  db/
    tables/         (11 table definition files)
    DatabaseFactory.kt

composeApp/src/jvmTest/kotlin/org/example/project/
  db/              (per-phase integration tests)
```

**14 tables total:** Characters, Currencies, CurrencyConversions, Transactions, Merchants, Products, ShippingMethods, MerchantShippingMethods, CartItems, WishlistItems, Orders, SubOrders, OrderItems, Reviews
