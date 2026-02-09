# Crypto Wallet Android App

A simple Android application with Web3 authentication and crypto wallet functionality using Dynamic SDK. Built for the Crypto Wallet Test Assignment.

## Overview

This app demonstrates basic blockchain operations on **Ethereum Sepolia** testnet:
- Email OTP authentication via Dynamic SDK
- View wallet address and balance (ETH)
- Send transactions on Sepolia

## Screens

### 1. Login Screen
- Email input field
- "Send OTP" button
- OTP verification bottom sheet (6-digit code)
- Error handling for invalid email, rate limits, failed OTP

### 2. Wallet Details Screen
- Displays EVM wallet address
- Current network: **Sepolia** (Chain ID 11155111)
- Balance in ETH (fetched from Sepolia RPC)
- **Copy Address** button
- **Send Transaction** button
- **Logout** button
- Pull-to-refresh for balance

### 3. Send Transaction Screen
- Recipient address input
- Amount input (ETH)
- **Send Transaction** button
- Loading / success / error states
- Transaction hash displayed on success
- Stays on screen after sending (can send again)

## Architecture

- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM
- **State**: Kotlin Coroutines + StateFlow
- **DI**: Hilt
- **Auth**: Dynamic SDK (Email OTP)
- **Blockchain**: Dynamic SDK EVM APIs, Web3j for Sepolia balance, Sepolia RPC

### Key Components
- `LoginScreen` / `LoginViewModel` – Email OTP auth with bottom sheet
- `WalletDetailsScreen` / `WalletDetailsViewModel` – Wallet info, balance, copy, logout
- `SendTransactionScreen` / `SendTransactionViewModel` – Send ETH on Sepolia
- `SepoliaRepository` – Balance fetching via Web3j + Sepolia RPC

## How to Run

### Prerequisites
- Android Studio (latest)
- JDK 17
- Android device/emulator (API 28+)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd MyApplication2
   ```

2. **Dynamic Account** (if using your own)
   - Create a project at [Dynamic.xyz](https://app.dynamic.xyz)
   - Copy your `environmentId`
   - Update `MainActivity.kt`:
     ```kotlin
     environmentId = "YOUR_ENVIRONMENT_ID"
     ```

3. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use Android Studio: Run → Run 'app'

### Get Test Tokens (Sepolia ETH)

1. **Google Cloud Faucet**
   - https://cloud.google.com/application/web3/faucet/ethereum/sepolia
   - Sign in with Google, paste your wallet address
   - Receive 0.05 Sepolia ETH

2. **Alchemy Faucet**
   - https://www.alchemy.com/faucets/ethereum-sepolia
   - Paste wallet address
   - Get up to 0.5 Sepolia ETH

**Check balance**: https://sepolia.etherscan.io/address/YOUR_ADDRESS

## Assumptions

1. **Dynamic dashboard**: Sepolia network is enabled for the project. If balance/transactions fail, ensure Sepolia is configured in the Dynamic dashboard.
2. **Embedded wallet**: Dynamic creates an embedded EVM wallet on first login. The wallet address is used for balance and send.
3. **RPC**: Public Sepolia RPC (`https://rpc.sepolia.org`) is used for balance. For production, consider a dedicated RPC provider (Alchemy, Infura, etc.).
4. **Redirect URL**: `myapp://auth` is configured in both `ClientProps` and `AndroidManifest.xml` for OAuth flows.

## Dependencies

- Dynamic SDK (`dynamic-sdk-android.aar`)
- Web3j (Sepolia balance)
- Jetpack Compose, Material3
- Hilt
- Kotlin Coroutines

## Screenshots

_Add screenshots of the 3 screens here after running the app._

## License

This project is for educational/test assignment purposes.
