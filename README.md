# DSBmaterial 🎓

<p align="center">
  <img src="assets/icon.png" alt="App Icon" width="128"/>
</p>

<p align="center">
  <strong>Material You Expressive alternative for the DSBmobile app</strong><br>
</p>

<div align="center">    
  <a href="https://github.com/WollyDev24/DSB_Material/LICENSE">
    <img alt="GitHub License" src="https://img.shields.io/github/license/WollyDev24/DSB_Material?logo=data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0ndXRmLTgnPz48c3ZnIHdpZHRoPSc4MDBweCcgaGVpZ2h0PSc4MDBweCcgdmlld0JveD0nMCAwIDI0IDI0JyBmaWxsPSdub25lJyB4bWxucz0naHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmcnPjxwYXRoIGQ9J00xOSAzSDlWM0M3LjExNDM4IDMgNi4xNzE1NyAzIDUuNTg1NzkgMy41ODU3OUM1IDQuMTcxNTcgNSA1LjExNDM4IDUgN1YxMC41VjE3JyBzdHJva2U9JyNGRkZGRkYnIHN0cm9rZS13aWR0aD0nMicgc3Ryb2tlLWxpbmVjYXA9J3JvdW5kJyBzdHJva2UtbGluZWpvaW49J3JvdW5kJy8+PHBhdGggZD0nTTE0IDE3VjE5QzE0IDIwLjEwNDYgMTQuODk1NCAyMSAxNiAyMVYyMUMxNy4xMDQ2IDIxIDE4IDIwLjEwNDYgMTggMTlWOVY0LjVDMTggMy42NzE1NyAxOC42NzE2IDMgMTkuNSAzVjNDMjAuMzI4NCAzIDIxIDMuNjcxNTcgMjEgNC41VjQuNUMyMSA1LjMyODQzIDIwLjMyODQgNiAxOS41IDZIMTguNScgc3Ryb2tlPScjRkZGRkZGJyBzdHJva2Utd2lkdGg9JzInIHN0cm9rZS1saW5lY2FwPSdyb3VuZCcgc3Ryb2tlLWxpbmVqb2luPSdyb3VuZCcvPjxwYXRoIGQ9J00xNiAyMUg1QzMuODk1NDMgMjEgMyAyMC4xMDQ2IDMgMTlWMTlDMyAxNy44OTU0IDMuODk1NDMgMTcgNSAxN0gxNCcgc3Ryb2tlPScjRkZGRkZGJyBzdHJva2Utd2lkdGg9JzInIHN0cm9rZS1saW5lY2FwPSdyb3VuZCcgc3Ryb2tlLWxpbmVqb2luPSdyb3VuZCcvPjxwYXRoIGQ9J005IDdIMTQnIHN0cm9rZT0nI0ZGRkZGRicgc3Ryb2tlLXdpZHRoPScyJyBzdHJva2UtbGluZWNhcD0ncm91bmQnIHN0cm9rZS1saW5lam9pbj0ncm91bmQnLz48cGF0aCBkPSdNOSAxMUgxNCcgc3Ryb2tlPScjRkZGRkZGJyBzdHJva2Utd2lkdGg9JzInIHN0cm9rZS1saW5lY2FwPSdyb3VuZCcgc3Ryb2tlLWxpbmVqb2luPSdyb3VuZCcvPjwvc3ZnPg==&color=43444B"></a>
  <a href="https://github.com/WollyDev24/DSB_Material/actions/workflows/build.yml">
    <img alt="GitHub Actions Workflow Status" src="https://img.shields.io/github/actions/workflow/status/WollyDev24/DSB_Material/build.yml?&logo=github&color=3F9E00"></a>
<!--  <a href="https://discord.gg/3x8qNWxgGZ">
    <img alt="Discord" src="https://img.shields.io/discord/803299970169700402?logo=discord&logoColor=white&label=Discord&color=5165f6"></a> -->
</div>

## This is the development branch

## ❓ Why does this app exist?
- The offical DSBmobile app sucks for various reasons (outdated ui, not reliable, just displays a webpage), so i decided to make my own app with an Material You UI, Class Filtering and Displaying content natively.

## ⬇️ Get DSBmaterial from here

<p align="left">
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/WollyDev24/DSB_Material">
    <img src="assets/obtainium.png" alt="Get it on Obtainium" height="60" /></a>
  <a href="https://github.com/WollyDev24/DSB_Material/releases/latest">
    <img src="assets/github.webp" alt="Get it on GitHub" height="60" /></a>
  <a href="https://fdroid.org">
    <img src="assets/fdroid.png" alt="Get it on Fdroid" height="60" /></a>
</p>

## 📂 Project Structure

```text
app/src/main/java/dev/wolly/dsbmaterial/
├── api/
│   └── DSBMobileAPI.kt       # API client for DSBmobile (with GZIP/HTML parsing)
├── data/
│   ├── DataStoreManager.kt   # Persistent storage for user settings & credentials
│   └── Models.kt             # Data classes for substitution entries
├── ui/
│   ├── theme/                # Material 3 Theme, Color, Type, and Shape definitions
│   └── MainViewModel.kt      # ViewModel handling business logic and UI state
└── MainActivity.kt           # Main entry point and all Compose UI screens
```

## 🛠️ Building the app from source:

1. **Clone the repo**
```bash
git clone https://github.com/WollyDev24/DSB_Material/
```
2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Sync and Build**
   - Wait for Gradle to sync dependencies
   - Build the project (Build → Make Project)

4. **Run**
   - Connect a device or start an emulator
   - Click Run (▶️)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## ⭐ Special Thanks to
- [Tenner/dsbmobile](https://github.com/Tenner/dsbmobile) - Understanding of the API

## 📄 License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.

---