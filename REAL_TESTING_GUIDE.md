# 🔥 NFT Tax Calculator Pro - PRODUCTION TESTING Guide

**Version:** 2.0.0 - December 2024  
**Testing Environment:** Production APIs Only
**Last Updated:** December 2024

## 💯 **TESTING CON DATI REALI DI PRODUZIONE**

**ENTERPRISE TESTING!** L'app usa **API di produzione** per testare con NFT e transazioni reali!

---

## 🎯 **Come Testare con i Tuoi NFT Reali**

### **1. Build l'APK con API Production**
```bash
# Push to develop = APK con API REALI
git push origin develop

# Download APK da GitHub Actions
# Installa sul tuo telefono
```

### **2. Test con i Tuoi Contract Address**

**📱 Apri l'app e testa:**

```bash
# I TUOI NFT CONTRACT REALI - Esempi:
0x60E4d786628Fea6478F785A6d7e704777c86a7c6  # Mutant Ape Yacht Club
0xBC4CA0EdA7647A8aB7C2061c2E118A18a936f13D  # Bored Ape Yacht Club  
0x1A92f7381B9F03f27588D17Fa109Aa4A13fffe9f  # OpenSea Shared Storefront
[IL TUO CONTRACT ADDRESS]  # I tuoi NFT
```

### **3. Verifica TUTTO Funziona:**

**✅ Test Checklist:**
- [ ] **Sync transactions** - Vedi le tue vendite reali?
- [ ] **Total revenue** - Cifre corrette vs OpenSea?  
- [ ] **Gas fees** - Calcoli giusti?
- [ ] **Tax calculation** - Percentuali OK?
- [ ] **Export CSV** - Dati esportano correttamente?
- [ ] **Multi-chain** - Ethereum + Polygon funzionano?

### **4. Confronta con OpenSea Dashboard**

```bash
# Vai su OpenSea.io
# Login con il tuo wallet
# Account → Activity
# Confronta con l'app:
# - Numero vendite ✅
# - Importi ✅  
# - Date ✅
# - Gas fees ✅
```

---

## 🔥 **Setup API Keys REALI**

### **OpenSea API (PRODUCTION):**
1. Vai su https://docs.opensea.io/reference/api-keys
2. Crea account developer  
3. **Genera API key MAINNET** (non testnet!)
4. Aggiungi a GitHub Secrets: `OPENSEA_API_KEY_PROD`

### **Alchemy API (PRODUCTION):**
1. Vai su https://alchemy.com
2. Crea app per **Ethereum Mainnet** 
3. Copia API key
4. Aggiungi a GitHub Secrets: `ALCHEMY_API_KEY_PROD`

### **Infura (PRODUCTION):**
1. Vai su https://infura.io
2. Crea progetto **Mainnet**
3. Copia Project ID
4. Aggiungi a GitHub Secrets: `INFURA_PROJECT_ID_PROD`

---

## 💰 **Test Revenue/Tax Calculations**

### **Verifica Calcoli Tasse:**

```kotlin
// L'app deve calcolare correttamente:
val totalRevenue = yourActualSales.sumOf { it.amount }
val capitalGains = totalRevenue * 0.8  // 80% capital gains
val italianTax = capitalGains * 0.26   // 26% tasse Italia

// FIFO/LIFO per ottimizzazione (Pro tier)
val optimizedTax = calculateWithFIFO(yourSales) // Meno tasse
```

### **Test con Diversi Scenari:**
```bash
# Test Case 1: Poche vendite (< €1000)
Contract: [un tuo contratto con poche vendite]
Expected: Calcolo semplice, tasse base

# Test Case 2: Molte vendite (> €10k)  
Contract: [contratto con molte transazioni]
Expected: FIFO/LIFO optimization disponibile

# Test Case 3: Multi-chain
Contract: [contratti su Ethereum + Polygon]
Expected: Aggregazione corretta cross-chain
```

---

## 🚨 **Problemi Comuni & Fix**

### **"No transactions found":**
```bash
# Possibili cause:
❌ Contract address sbagliato
❌ API key limitata/scaduta  
❌ Rate limiting API
❌ Contratto su chain sbagliata

# Fix:
✅ Verifica contract su Etherscan
✅ Controlla API quotas
✅ Usa contratti con vendite confermate
```

### **"Revenue doesn't match OpenSea":**
```bash
# Possibili cause:
❌ API non sincronizzate
❌ Filtri diversi (pending vs completed)
❌ Timeframe diverso
❌ Currency conversion

# Fix:  
✅ Confronta stesso periodo
✅ Solo transazioni "completed"
✅ Check currency (ETH vs USD)
```

### **"Tax calculation seems wrong":**
```bash
# Verifica manualmente:
Total Sales: €50,000
Capital Gains (80%): €40,000  
Italian Tax (26%): €10,400
Final Tax: €10,400

# Se diverso = bug nel calcolo
```

---

## 🎯 **Real World Test Scenarios**

### **Scenario 1: Whale Trader**
```bash
Contract: Un grosso contratto NFT con centinaia di vendite
Expected Results:
- Revenue > €100k
- Hundreds of transactions
- Complex tax optimization needed
- CSV export with all data
```

### **Scenario 2: Casual Collector**  
```bash
Contract: Contratto con 5-10 vendite
Expected Results:
- Revenue €1k-€5k
- Simple tax calculation
- Basic export functionality
```

### **Scenario 3: Multi-Chain User**
```bash
Contracts: 
- Ethereum: 0xabc123...
- Polygon: 0xdef456...  
Expected Results:
- Both chains sync correctly
- Combined P&L calculation
- Cross-chain tax optimization
```

---

## 📊 **Performance Benchmarks**

### **Durante il Test, Misura:**
```bash
⏱️  Sync Time: < 30 secondi per 100 transazioni
📱 App Size: < 15MB installato  
🔋 Battery: < 5% consumo per sync completo
💾 Storage: < 50MB dati cached
📶 Network: < 10MB data per sync
```

---

## ✅ **Final Testing Approval**

**Prima di uppare su Play Store:**

- [ ] **✅ Revenue calculations match OpenSea exactly**
- [ ] **✅ All your NFT contracts sync correctly** 
- [ ] **✅ Tax calculations are accurate**
- [ ] **✅ Export includes all your real transactions**
- [ ] **✅ App performance is smooth with real data**
- [ ] **✅ Multi-chain works with your wallets**
- [ ] **✅ No crashes with large transaction volumes**

**SOLO QUANDO TUTTO FUNZIONA CON I TUOI NFT REALI → upload su Play Store!**

---

## 🔥 **Bottom Line**

**Testing = API Production + Dati Reali**

Non puoi testare un tax calculator NFT con dati fake! Deve funzionare con i tuoi NFT veri o non serve a niente! 💯