# 🔐 GitHub Secrets Configuration

## Required Secrets for NFT Pro Build

### 🔑 **Signing Configuration**

```bash
# Generate release keystore (run once)
keytool -genkey -v -keystore nft-pro-release.keystore \
        -alias nft-pro \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass YOUR_STORE_PASSWORD \
        -keypass YOUR_KEY_PASSWORD

# Convert to base64 for GitHub secret
base64 -i nft-pro-release.keystore | pbcopy
```

**Required Signing Secrets:**
- `RELEASE_KEYSTORE` - Base64 encoded keystore file
- `RELEASE_KEYSTORE_PASSWORD` - Keystore password  
- `RELEASE_KEY_ALIAS` - Key alias (e.g., "nft-pro")
- `RELEASE_KEY_PASSWORD` - Key password

### 📱 **Firebase Configuration**

**Production Environment:**
- `GOOGLE_SERVICES_JSON_PROD` - Production google-services.json content

**Staging Environment:**
- `GOOGLE_SERVICES_JSON_STAGING` - Staging google-services.json content

### 🌐 **API Keys**

**Production APIs:**
```bash
OPENSEA_API_KEY_PROD=your_opensea_production_key
ALCHEMY_API_KEY_PROD=your_alchemy_production_key
INFURA_PROJECT_ID_PROD=your_infura_production_id
REVENUECAT_API_KEY_PROD=your_revenuecat_production_key
```

**Testing APIs:**
```bash
OPENSEA_API_KEY_TEST=your_opensea_test_key
ALCHEMY_API_KEY_TEST=your_alchemy_test_key
INFURA_PROJECT_ID_TEST=your_infura_test_id
REVENUECAT_API_KEY_TEST=your_revenuecat_test_key
```

### 🎯 **Google Play Console (Optional)**

If you decide to enable auto-deploy later:
- `GOOGLE_PLAY_SERVICE_ACCOUNT` - Service account JSON for Play Console

## 📋 **Setup Instructions**

### 1. Go to GitHub Repository Settings
```
Repository → Settings → Secrets and variables → Actions
```

### 2. Add Each Secret
Click "New repository secret" and add all the secrets above.

### 3. Test the Build
Push to `develop` branch to trigger a test build:
```bash
git checkout develop
git push origin develop
```

### 4. Production Build
Push to `main` or create a tag:
```bash
# For main branch build
git checkout main
git push origin main

# For release build
git tag v1.0.0
git push origin v1.0.0
```

## 📥 **Download Built Files**

After successful build:

1. **Go to Actions tab** in GitHub repository
2. **Click on completed workflow run**
3. **Download artifacts:**
   - `📱-nft-pro-testing-apk-XXXX` - For testing
   - `🏪-nft-pro-production-aab-XXXX` - For Play Store upload
   - `📱-nft-pro-production-apk-XXXX` - For manual testing
   - `📋-nft-pro-mapping-XXXX` - ProGuard mapping file

## ⚠️ **Important Notes**

- **NO automatic deployment** - you control when to upload to Play Store
- **Test APK first** before uploading AAB to Play Store
- **Keep mapping files** for crash analysis
- **All builds are signed** with production certificate

## 🔧 **Manual Play Store Upload**

1. **Test the APK** thoroughly on real devices
2. **Login to Google Play Console**
3. **Go to NFT Pro app → Release → Production**
4. **Upload the AAB file** (not APK)
5. **Upload mapping.txt** for crash reports
6. **Set rollout percentage** (start with 5-10%)
7. **Submit for review**

## 🚨 **Security Best Practices**

- ✅ Never commit secrets to code
- ✅ Use different API keys for test/prod
- ✅ Rotate secrets regularly
- ✅ Monitor API usage and costs
- ✅ Keep keystore backup in secure location