# Mealoo WhatsApp Order Flow

```mermaid
flowchart TD
    START([Customer sends Hi/Hello/Hey]) --> SR

    SR[/"🍽️ SELECT_RESTAURANT\nShow 5 restaurants"/]
    SR -->|Valid number 1-5| SC
    SR -->|Invalid input| SR

    SC[/"📋 SELECT_CATEGORY\nShow categories for restaurant"/]
    SC -->|Valid number| SI
    SC -->|Invalid input| SC

    SI[/"🍱 SELECT_ITEMS\nShow menu items with prices"/]
    SI -->|Valid number| RC
    SI -->|Invalid input| SI

    RC[/"🛒 REVIEW_CART\nShow items + total\n1️⃣ Add More  2️⃣ Checkout"/]
    RC -->|1 - Add More| SC
    RC -->|2 - Checkout| CA
    RC -->|Invalid input| RC

    CA[/"📍 COLLECT_ADDRESS\nAsk for delivery address"/]
    CA -->|Address too short < 10 chars| CA
    CA -->|Valid address| CO

    CO[/"📄 CONFIRM_ORDER\nShow full order summary\n1️⃣ Confirm  2️⃣ Cancel"/]
    CO -->|1 - Confirm| PS
    CO -->|2 - Cancel| CANCELLED([Order Cancelled → back to restaurants])
    CO -->|Invalid input| CO

    PS[/"💳 PAYMENT_SELECTION\n1️⃣ UPI  2️⃣ COD"/]
    PS -->|1 - UPI| UPI
    PS -->|2 - COD| PLACE_COD
    PS -->|Invalid input| PS

    UPI[/"💳 AWAITING_UPI_PAYMENT\nShow UPI ID: mealoo@upi\nWait for backend confirmation"/]
    UPI -->|Backend confirms payment| OP_UPI
    UPI -->|Customer message| UPI

    PLACE_COD -->|Auto proceed| OP_COD

    PLACE_COD[/Place Order - COD/]

    OP_UPI[/"✅ ORDER_PLACED\nOrder ID: M10xxx\nPayment: UPI\nETA: 35 minutes"/]
    OP_COD[/"✅ ORDER_PLACED\nOrder ID: M10xxx\nPayment: COD\nETA: 35 minutes"/]

    OP_UPI -->|Hi/Hello/Hey| SR
    OP_COD -->|Hi/Hello/Hey| SR
    OP_UPI -->|Any other message| OP_UPI
    OP_COD -->|Any other message| OP_COD

    style START fill:#25D366,color:#fff
    style CANCELLED fill:#e74c3c,color:#fff
    style OP_UPI fill:#27ae60,color:#fff
    style OP_COD fill:#27ae60,color:#fff
    style UPI fill:#f39c12,color:#fff
```

## States

| State | Trigger | Next State |
|---|---|---|
| `SELECT_RESTAURANT` | Hi / Hello / Hey | — |
| `SELECT_CATEGORY` | Valid restaurant number | — |
| `SELECT_ITEMS` | Valid category number | — |
| `REVIEW_CART` | Valid item number | — |
| `COLLECT_ADDRESS` | Send `2` from cart | — |
| `CONFIRM_ORDER` | Address entered | — |
| `PAYMENT_SELECTION` | Send `1` to confirm | — |
| `AWAITING_UPI_PAYMENT` | Send `1` (UPI) | — |
| `ORDER_PLACED` | COD auto / UPI backend confirm | — |

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/webhook/whatsapp` | Twilio incoming message |
| `POST` | `/webhook/payment/confirm?phone=` | Backend UPI payment confirmation |

## Error Handling

- Invalid menu selection → repeat current step
- Address < 10 chars → ask again
- UPI: order only placed after backend confirms payment
- Greetings at any state → reset session and restart
