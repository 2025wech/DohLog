---

## Supported SMS Formats

### M-Pesa (Safaricom Kenya)
- Received money
- Sent money
- Buy Goods (Till)
- Pay Bill
- Withdraw
- Airtime
- Deposit

### Airtel Money (Airtel Kenya)
- Received money — `TID:XXXX. Received Ksh X from NAME...`
- Sent money
- Bundle purchase
- Airtime
- Payment
- Withdrawal
- Deposit

---

## Permissions

| Permission | Purpose |
|---|---|
| `READ_SMS` | Import existing M-Pesa & Airtel transaction history |
| `RECEIVE_SMS` | Capture new transactions in real time |
| `USE_BIOMETRIC` | Fingerprint / PIN authentication |

> All SMS processing is done entirely on-device. 
> No data ever leaves your phone.

---

## Getting Started

1. Clone the repository
```bash
   git clone https://github.com/yourusername/autoledger.git
```

2. Open in Android Studio (Hedgehog or later)

3. Build and run on a physical device  
   *(SMS reading requires a real device with an active 
   M-Pesa or Airtel Money SIM)*

4. Grant SMS permissions on first launch

5. AutoLedger will automatically import the last 24 hours 
   of M-Pesa and Airtel Money messages as a starting point, 
   then capture all new transactions in real time going forward.

---

## Requirements

- Android 8.0+ (API 26+)
- Physical device with M-Pesa or Airtel Money SIM
- Kotlin 2.0.20+
- Android Studio Hedgehog+

---

## Privacy Policy

This AutoLedger does not collect, transmit, or store any data outside 
your device. SMS messages are read locally, parsed locally, and 
stored locally in a Room SQLite database. No analytics, no ads, 
no tracking.

---

## Developer

**Waweru**  
Android Developer — Built with ❤️ in Kenya 🇰🇪

---

## License
