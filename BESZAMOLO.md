# Számlakezelő Android Mobilalkalmazás – Fejlesztési Beszámoló

**Tantárgy:** Okostelefonok és IoT  
**Fejlesztő:** Nagy Máté (U3ROFS)  
**Dátum:** 2026. május  
**Technológia:** Android (Kotlin, Jetpack Compose)  
**Projekt neve:** Invoice Store  

---

## 1. Bevezetés és motiváció

### 1.1 A projekt háttere

A féléves projektfeladat keretében egy Android mobilalkalmazást fejlesztettem, amelynek neve **Invoice Store**. Az alkalmazás célja a vállalkozások mindennapi számlakezelési folyamatainak digitális, mobilbarát megvalósítása. A feladat elvégzése során arra törekedtem, hogy egy valódi, éles körülmények között is használható megoldást hozzak létre, ne csupán egy oktatási prototípust, amely a laboron kívül sohasem kerülne alkalmazásra.

Az ötlet abból a megfigyelésből fakad, hogy a kis- és középvállalkozások, egyéni vállalkozók és szabadúszók körében a számlakezelés ma még gyakran rendkívül nehézkes: vagy papír alapon történik, ami kereshetetlen és elveszhet, vagy asztali számlázó szoftvert használnak, amelyhez mobilon nem lehet hozzáférni. Egy mobilalkalmazás ebben a kontextusban valódi értéket teremthet: a felhasználó útközben, azonnal ki tud állítani egy számlát, rögzítheti a vevő adatait, csatolhatja a teljesítésigazolást, és az adatok azonnal, felhőben szinkronizálódnak.

### 1.2 A megoldott üzleti probléma

A mobilos számlakezelés számos kihívást vet fel, amelyeket a fejlesztés során mind kezelni kellett:

- **Azonosítás és adatvédelem:** Minden felhasználónak kizárólag a saját számláihoz szabad hozzáférnie, akkor is, ha az adatok közös felhős adatbázisban tárolódnak.
- **Fájlmellékletek kezelése:** A számlákhoz kapcsolódó dokumentumokat (teljesítésigazolás, szerződés, szállítólevél) biztonságosan és hatékonyan kell tárolni és visszakereshetővé tenni.
- **Valós idejű szinkronizáció:** Ha a felhasználó egy számlát módosít, annak azonnal tükröződnie kell a többi eszközön is.
- **Automatikus számítások:** Az ÁFA kiszámítása és a bruttó összeg meghatározása ne a felhasználóra háruljon.
- **Magyar jogszabályi megfelelés:** A hazai adókulcsok (27% általános ÁFA) és fizetési módok (átutalás, készpénz, bankkártya, utánvét) beépítése szükséges.

### 1.3 Célkitűzések

A projekt kezdetén a következő konkrét célkitűzéseket fogalmaztam meg:

1. Biztonságos, kétféle módszert (e-mail/jelszó és Google-fiók) támogató autentikáció megvalósítása.
2. Teljes körű CRUD (Create, Read, Update, Delete) műveletek biztosítása számlákra.
3. Tételes számlák összeállíthatósága automatikus 27%-os ÁFA-számítással.
4. Fájlmellékletek (kép, PDF) csatolásának lehetősége számlákhoz.
5. Valós idejű adatszinkronizáció felhőalapú adatbázis segítségével.
6. Modern, anyagszerű (Material Design 3) felhasználói felület megvalósítása.
7. Adaptív elrendezés, amely mind telefonon, mind tableten megfelelően jelenik meg.

---

## 2. Tervezési fázis

### 2.1 Technológiai stack kiválasztása

A fejlesztés megkezdése előtt alaposan megvizsgáltam a rendelkezésre álló Android-fejlesztési eszközöket és a projekt igényeit, majd tudatos döntéseket hoztam az alkalmazandó technológiákról.

**UI keretrendszer:** A Jetpack Compose és a hagyományos XML/View alapú rendszer közötti választásnál a Compose mellett döntöttem. Ennek oka, hogy a Compose deklaratív paradigmája sokkal kevesebb boilerplate kódot igényel, az állapotkezelés természetesebb, és a Google is ezt az utat jelöli ki Android-fejlesztés jövőjeként. Az XML-alapú megközelítéssel dolgozva minden egyes UI-elemhez `findViewById` hívásokat, adapter osztályokat és manuális nézet-frissítéseket kellett volna írni – mindez a Compose-ban eltűnik.

**Backend és adatbázis:** A Firebase Platform kézenfekvő választásnak bizonyult: ingyenes szinten (Spark Plan) elegendő kvótát nyújt egy tanulmányi alkalmazáshoz, az Android SDK integrációja kiváló, és a Cloud Firestore NoSQL adatbázis különösen jól illeszkedik a dokumentum-orientált számlaadat-modellhez. Az SQLite-alapú lokális adatbázis (Room) alternatívát elvetettem, mivel az nem biztosít felhőszinkronizációt és többeszközös hozzáférést.

**Fájltárolás:** A Firebase Storage helyett a MinIO-t választottam, mert az lehetővé teszi az S3-kompatibilis API megismerését úgy, hogy semmilyen felhőköltséget nem generál fejlesztési fázisban. A MinIO ugyanolyan API-t nyújt, mint az AWS S3, így a tudás könnyen transzferálható éles környezetre is.

### 2.2 Adatmodell tervezése

Az adatmodell tervezésekor a valódi számlák struktúráját vettem alapul. Egy hiteles számladokumentumnak tartalmaznia kell az eladó és a vevő azonosító adatait, a számla egyedi sorszámát, a teljesítés és fizetési határidő dátumait, a tételeket egységárakkal és adótartalmakkal, valamint az összesítő sorokat. Ezeket az igényeket az `Invoice` és `InvoiceLineItem` adatosztályok tartalmazzák.

A Firestore NoSQL természetéből adódóan a számlák dokumentumként tárolódnak, a tételek beágyazott objektumok tömbjeként. Ez elkerüli a relációs adatbázisokban szükséges JOIN-okat, és a számla egésze egyetlen dokumentum-lekéréssel beolvasható.

### 2.3 Képernyőtérkép

A tervezési fázisban meghatároztam az alkalmazás képernyőstruktúráját:

```
Invoice_storeApp
├── AuthScreen (ha nincs bejelentkezve)
│   ├── Login mód (e-mail + jelszó)
│   ├── Sign Up mód (e-mail + jelszó + megerősítés)
│   └── Google Sign-In
└── MainScreen (ha be van jelentkezve)
    ├── INVOICES navigációs cél
    │   ├── InvoiceListScreen (lista mód)
    │   └── AddInvoiceScreen (létrehozás / szerkesztés mód)
    └── PROFILE navigációs cél
        └── ProfileScreen
```

Ez az egyszerű, lapos hierarchia tudatos döntés: az alkalmazás nem igényel mély navigációs fát, a két fő funkcióterület (számlák kezelése és profil) egy szintű navigációval elérhető.

---

## 3. Fejlesztési környezet

### 3.1 Build konfiguráció

Az alkalmazást **Android Studio** legújabb stabil verziójában fejlesztettem. A projekt build rendszere a **Gradle Version Catalog** megközelítést alkalmazza: a függőségek és verziók a `gradle/libs.versions.toml` fájlban centralizáltan vannak definiálva, és a `app/build.gradle.kts` Kotlin DSL szkriptben typesafe hozzáférőkkel hivatkozom rájuk. Ez a megközelítés megkönnyíti a verziók egységes kezelését és a függőségi konfliktusok elkerülését.

A build konfiguráció főbb jellemzői:

| Beállítás | Érték | Megjegyzés |
|---|---|---|
| AGP (Android Gradle Plugin) | 9.2.0 | Legújabb stabil |
| Kotlin | 2.2.10 | K2 compiler |
| Compile SDK | 36 | Android 16 preview |
| Target SDK | 35 | Android 15 |
| Min SDK | 24 | Android 7.0, ~95% eszközlefedettség |
| Compose BOM | 2025.12.00 | Összes Compose lib összehangolt verziója |
| Firebase BOM | 34.12.0 | Összes Firebase lib összehangolt verziója |

A **BOM (Bill of Materials)** használata mindkét ökoszisztémában (Compose és Firebase) azért előnyös, mert egyetlen verziószámot kell naprakészen tartani, és a BOM gondoskodik arról, hogy az összes kapcsolódó könyvtár kompatibilis verziót kapjon.

### 3.2 Kotlin 2 és a Compose Compiler

A Kotlin 2.2.10 a K2 compilert használja, amely jelentősen gyorsabb fordítási időt és jobb típuskövetkeztetést biztosít. A Compose Compiler plugin a Kotlin verzióval együtt mozog (`kotlinCompilerExtensionVersion` a BOM-ból kerül ki), így a két komponens mindig kompatibilis egymással. A `kotlin("plugin.compose")` plugin az `app/build.gradle.kts`-ben van aktiválva.

### 3.3 Futtatási környezet

A fejlesztés és tesztelés Android Emulatoron (Pixel 8, API 35) történt. A MinIO objektumtár Docker-konténerben fut a gazdagépen, amelyet az emulátorból a `10.0.2.2` IP-cíemen érek el – ez az Android Emulator speciális bridge-IP-je, amely a gazdagép `localhost`-ját jelöli.

---

## 4. Alkalmazásarchitektúra

### 4.1 Single-Activity megközelítés

Az alkalmazás egyetlen `Activity`-ből (`MainActivity`) áll, amely a modern Android-fejlesztési ajánlásokat követi. A hagyományos több-Activity megközelítéssel szemben az egyetlen Activity-ben Compose-os navigációval megvalósított struktúra egyszerűbb életciklus-kezelést, gördülékenyebb átmeneteket és konzisztensebb állapotmegosztást tesz lehetővé a képernyők között.

Az `onCreate` metódusban mindössze két fontos hívás történik: az `enableEdgeToEdge()`, amely a teljes képernyős megjelenítési módot aktiválja, és a `setContent`, amely belép a Compose UI-ba:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Invoice_storeTheme {
                Invoice_storeApp()
            }
        }
    }
}
```

Az `Invoice_storeTheme` wrapper gondoskodik az alkalmazás vizuális témájának alkalmazásáról (Material 3, dinamikus szín, sötét mód) az összes belső composable számára.

### 4.2 Gyökér composable és autentikációs routing

Az `Invoice_storeApp` composable az alkalmazás belépési pontja. Ez a függvény figyeli a Firebase autentikációs állapotot, és ennek alapján dönt, melyik fő képernyőt jelenítse meg:

```kotlin
@Composable
fun Invoice_storeApp() {
    var currentUser by remember { mutableStateOf(FirebaseManager.auth.currentUser) }

    if (currentUser == null) {
        AuthScreen(onAuthSuccess = {
            currentUser = FirebaseManager.auth.currentUser
        })
    } else {
        MainScreen(onLogout = {
            FirebaseManager.auth.signOut()
            currentUser = null
        })
    }
}
```

Ez a struktúra az **egyirányú adatfolyam** (Unidirectional Data Flow, UDF) elvét alkalmazza: az állapot (`currentUser`) fentről lefelé áramlik a composable fába, az események (bejelentkezés, kijelentkezés) pedig alulról felfelé, callback lambdákon keresztül jutnak vissza. Ez az architektúra megakadályozza a kétirányú, nehezen követhető állapotmutációt, és megkönnyíti a komponensek egymástól független tesztelhetőségét.

### 4.3 FirebaseManager singleton

A Firebase szolgáltatások elérését egy `FirebaseManager` singleton objektum közvetíti. Ez a minta biztosítja, hogy az alkalmazásban bárhol, egyszerűen elérhető legyen az autentikáció és az adatbázis példánya, anélkül hogy a `Context`-et minden hívási helyig propagálni kellene. A Firebase SDK maga is singleton-alapú, így ez a minta jól illeszkedik a meglévő architektúrához.

### 4.4 Adatfolyam a MainScreen-ben

A `MainScreen` composable feladata az adatlekérés koordinálása és az állapot terjesztése a gyermek composable-ök felé. A Firestore snapshot-listener beindítása `LaunchedEffect(userId)` blokkban történik: ez biztosítja, hogy a listener pontosan egyszer indul el (az első kompozíció során), és ha a `userId` megváltozna (pl. más felhasználó jelentkezik be), a listener újraindul.

```kotlin
LaunchedEffect(userId) {
    FirebaseManager.db.collection("invoices")
        .whereEqualTo("userId", userId)
        .addSnapshotListener { snapshot, e ->
            isLoading = false
            if (e != null) {
                Toast.makeText(context, "Error loading: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                invoices = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Invoice::class.java)?.copy(id = doc.id)
                }
            }
        }
}
```

Az `invoices` állapotváltozó egy `List<Invoice>`, amelyet a `MainScreen` átad az `InvoiceListScreen`-nek és a `ProfileScreen`-nek. A Firestore listener valós időben értesíti az alkalmazást minden adatbázis-változásról, ezért nincs szükség manuális pull-to-refresh mechanizmusra.

---

## 5. Felhasználói azonosítás

### 5.1 A Firebase Authentication szerepe

A felhasználói azonosítás megvalósítása az egyik legkritikusabb biztonsági aspektus az alkalmazásban, hiszen az adatok felhőben tárolódnak, és szigorúan el kell különíteni az egyes felhasználók adatait egymástól. A **Firebase Authentication** ezt a problémát elegánsan oldja meg: minden bejelentkezési kísérlet után egy `FirebaseUser` objektumot ad vissza, amelynek `uid` mezője egy globálisan egyedi azonosító. Ezt az azonosítót tárolom minden számla `userId` mezőjében, és a Firestore lekérésnél szűrőként alkalmazom.

A Firebase Auth további előnye, hogy a jelszavak sohasem tárolódnak az alkalmazásban vagy az adatbázisban – azokat a Firebase szerverei kezelik bcrypt hash-eléssel. A fejlesztőnek a jelszókezeléssel egyáltalán nem kell foglalkoznia.

### 5.2 E-mail és jelszó alapú autentikáció

Az `AuthScreen` composable kétféle módban működik, amelyek közötti váltást egy `isLoginMode` boolean állapotváltozó vezérli. A mód megváltozásakor a Compose automatikusan újrarajzolja az érintett részeket (megjelenik vagy eltűnik a jelszó-megerősítő mező, megváltozik a gomb felirata és a lenti átváltó szöveg).

Bejelentkezéskor a `signInWithEmailAndPassword` Firebase-metódust hívom meg, amelynek eredményét egy `addOnSuccessListener` / `addOnFailureListener` párban kezelem. Sikerkor az `onAuthSuccess` callback meghívásával értesítem a szülő composable-t, sikertelenség esetén a Firebase `AuthException` üzenetét megjelenítem a felhasználónak.

Regisztrációkor két különleges ellenőrzést végzek az egyébként szintén Firebase-alapú `createUserWithEmailAndPassword` hívás előtt:
- A két jelszómező tartalma megegyezik-e?
- A jelszó kellően hosszú-e? (A Firebase maga is visszautasítja a 6 karakternél rövidebb jelszavakat, de a helyi validáció gyorsabb visszajelzést ad.)

### 5.3 Google Sign-In az AndroidX Credentials API-val

A Google-fiókkal való bejelentkezés implementációja technikailag az egyik legérdekesebb része a projektnek, mivel itt több könyvtár és protokoll működik együtt.

Az **AndroidX Credentials API** (v1.6.0) egy egységes, modern keretrendszer a különféle hitelesítő adatok (jelszavak, passkey-ek, Google-fiókok) kezelésére. Az idősebb `GoogleSignIn` API-val szemben ez a megközelítés jövőbiztosabb, és jobban integrálódik az Android biometria-rendszerével.

A folyamat részletesen:

**1. lépés – Kérés összeállítása:**
```kotlin
val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)
    .setServerClientId(context.getString(R.string.web_client_id))
    .setAutoSelectEnabled(true)
    .build()

val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()
```
A `setFilterByAuthorizedAccounts(false)` lehetővé teszi, hogy nemcsak a korábban már használt Google-fiókok jelenjenek meg opcióként. A `setAutoSelectEnabled(true)` beállítással visszatérő felhasználóknál az egyetlen elérhető fiók automatikusan kiválasztódik, nem kell manuálisan kattintani.

**2. lépés – Credential Manager hívás:**
```kotlin
val result = CredentialManager.create(context).getCredential(context, request)
val credential = result.credential as GoogleIdTokenCredential
val idToken = credential.idToken
```
Ez a hívás a rendszer credential picker UI-ját nyitja meg. A visszakapott `GoogleIdTokenCredential`-ből kinyerem az ID-tokent, amelyet a Google szerverei írtak alá – ez a token bizonyítja, hogy a felhasználó valóban az adott Google-fiók tulajdonosa.

**3. lépés – Firebase autentikáció:**
```kotlin
val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
FirebaseManager.auth.signInWithCredential(firebaseCredential)
    .addOnSuccessListener { onAuthSuccess() }
```
A Google által aláírt ID-tokent átadom a Firebase-nek, amely ellenőrzi azt a Google szerverein, majd létrehozza (vagy belép) a Firebase felhasználói fiókba. Ez után a `FirebaseManager.auth.currentUser` már nem null, és az alkalmazás átlép a `MainScreen`-re.

### 5.4 Munkamenet-perzisztencia

A Firebase Authentication automatikusan persistál egy refresh-tokent az eszközön (SharedPreferences-ben). Így az alkalmazás következő indításakor a `currentUser` rögtön elérhető, nincs szükség újbóli bejelentkezésre. A token lejáratakor (tipikusan egy óra az access token, hónapok a refresh token esetén) a Firebase automatikusan megújítja azt.

---

## 6. Adatmodell

### 6.1 Az Invoice adatosztály

A számla teljes adatstruktúráját egyetlen Kotlin `data class` foglalja össze. Az adatosztály tervezésekor három szempontot kellett egyensúlyban tartani: a Firestore deszerlializáció igényeit (paraméter nélküli konstruktor szükséges, amelyet az összes mező alapértékkel való ellátásával érek el), a valódi számlák tartalmi követelményeit, és a kód olvashatóságát.

```kotlin
data class Invoice(
    val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000),
    val sellerName: String = "",
    val sellerAddress: String = "",
    val sellerTaxId: String = "",
    val sellerBankName: String = "",
    val sellerBankAccount: String = "",
    val customerName: String = "",
    val customerAddress: String = "",
    val customerTaxId: String = "",
    val paymentMethod: String = "Átutalás",
    val currency: String = "HUF",
    val items: List<InvoiceLineItem> = emptyList(),
    val totalNet: Double = 0.0,
    val totalVat: Double = 0.0,
    val totalGross: Double = 0.0,
    val attachmentUrl: String = "",
    val attachmentName: String = ""
)
```

Fontos megfigyelés az `id` mező kezelése: a Firestore dokumentumok azonosítója (`documentId`) nem szerepel a dokumentum mezői között, ezért azt a betöltés után kézzel kell bemásolni a `copy(id = doc.id)` hívással. Ez egy bevett minta Firestore + Kotlin data class kombinációban.

A `dueDate` alapértéke 7 nappal a számla kiállítása utáni időpont, ami megfelel a leggyakoribb üzleti gyakorlatnak. A számítás milliszekundumban történik: `7 * 24 * 60 * 60 * 1000`.

### 6.2 Az InvoiceLineItem adatosztály

A számlatételek beágyazott objektumok a számla dokumentumában:

```kotlin
data class InvoiceLineItem(
    val description: String = "",
    val quantity: Double = 0.0,
    val unit: String = "db",
    val unitPrice: Double = 0.0,
    val netPrice: Double = 0.0,
    val vatRate: Double = 27.0,
    val vatAmount: Double = 0.0,
    val grossPrice: Double = 0.0
)
```

Az alapértelmezett mértékegység `"db"` (darab), az alapértelmezett ÁFA-kulcs `27.0` – mindkettő a magyar üzleti kontextusnak megfelelő értéket képvisel. A `vatRate` mezőt azért tárolom a tételben is (nem csak a számításhoz alkalmazom), mert a jövőbeli bővítmény lehetőséget adhatna eltérő ÁFA-kulcsú tételekre (pl. 5%-os vagy 18%-os kedvezményes kulcs).

### 6.3 Szerializáció

A Firestore-ba való mentéshez mindkét osztályban van `toMap()` metódus. Ez a megközelítés explicit kontrollt ad a Firestore-ba kerülő adatok felett – ellentétben az automatikus szerializációval, ahol véletlenül is kerülhetne felesleges vagy érzékeny mező az adatbázisba. A tételek rekurzívan szerializálódnak:

```kotlin
fun toMap(): Map<String, Any?> {
    return mapOf(
        "userId" to userId,
        "invoiceNumber" to invoiceNumber,
        "date" to date,
        "dueDate" to dueDate,
        "sellerName" to sellerName,
        "sellerAddress" to sellerAddress,
        "sellerTaxId" to sellerTaxId,
        "sellerBankName" to sellerBankName,
        "sellerBankAccount" to sellerBankAccount,
        "customerName" to customerName,
        "customerAddress" to customerAddress,
        "customerTaxId" to customerTaxId,
        "paymentMethod" to paymentMethod,
        "currency" to currency,
        "items" to items.map { it.toMap() },
        "totalNet" to totalNet,
        "totalVat" to totalVat,
        "totalGross" to totalGross,
        "attachmentUrl" to attachmentUrl,
        "attachmentName" to attachmentName
    )
}
```

Megfigyelendő, hogy az `id` mező szándékosan hiányzik a `toMap()` kimenetéből – az a Firestore dokumentum azonosítója, és nem egy adatmező.

### 6.4 Adatbázis struktúra a Firestore-ban

A Firestore-ban egyetlen `invoices` kollekció tárolja az összes felhasználó összes számláját. Az elkülönítés nem fizikai (különböző kollekcióban), hanem logikai szinten történik: minden dokumentum tartalmaz egy `userId` mezőt, és a lekérések mindig erre szűrnek. Ez a megközelítés egyszerűbb, mint felhasználónkénti alkollekciókat fenntartani, de Firestore Security Rules-szal kiegészítve ugyanolyan biztonságos.

---

## 7. Felhasználói felület

### 7.1 Vizuális tématervezés

Az alkalmazás vizuális arculatát a **Material Design 3** rendszer határozza meg. Az `Invoice_storeTheme` composable az `ui/theme` csomagban definiált színpalettát alkalmazza, amely lila (purple) alapú. Az Android 12+ rendszereken a **Dynamic Color** funkció is be van kapcsolva: ha a felhasználó engedélyezi, az alkalmazás átveszi a háttérkép alapján generált rendszerszíneket, és azokkal jeleníti meg a felületet.

A sötét mód automatikusan érzékelésre kerül a rendszer beállítása alapján (`isSystemInDarkTheme()`), és az összes szín, felület és szöveg szín ennek megfelelően vált át. Külön sötét módú erőforrásokat nem kellett létrehoznom – a Material 3 tokenalapú szín rendszere automatikusan kezeli ezt.

### 7.2 Adaptív navigáció

A fő képernyő navigációjához a `NavigationSuiteScaffold` komponenst választottam, amely a `androidx.compose.material3.adaptive.navigationsuite` csomagból származik. Ez a komponens automatikusan felismeri az ablak méretosztályát, és ennek megfelelően választja meg a navigációs sáv típusát:

- **Kompakt ablakon (telefon álló módban):** Alul megjelenő navigációs sáv (`NavigationBar`)
- **Közepes ablakon (telefon fekvő módban vagy kis tablet):** Oldalsó navigációs sáv (`NavigationRail`)
- **Tágas ablakon (nagy tablet):** Állandó navigációs fiók (`NavigationDrawer`)

Ez a viselkedés teljesen automatikus, nem kellett különálló layout fájlokat írnom a különböző képernyőméretekhez. A navigációs célokat egy `enum class` írja le, amelynek minden eleme tartalmaz egy `label` szöveget és egy `icon` vektorikonj értéket.

### 7.3 Hármas állapotú listanézet

Az `InvoiceListScreen` három különböző vizuális állapotban képes megjelenni, amelyek kezelése egyszerű feltételes elágazásokkal valósul meg:

**Töltési állapot:** Amíg a Firestore adatlekérés folyamatban van, a képernyő közepén egy `CircularProgressIndicator` forog. Az `isLoading` állapotváltozó `true` értékkel indul, és a snapshot-listener első meghívásakor áll `false`-ra (akkor is, ha üres a lista vagy hiba történt).

**Üres állapot:** Ha a lekérés sikeresen lefutott, de a felhasználónak még nincs egy számlája sem, egy tájékoztató szöveg jelenik meg a képernyő közepén, amely egyben instrukcióként is szolgál: „No invoices found. Click + to add one." Ez megakadályozza, hogy a felhasználó üres képernyő előtt álljon útmutatás nélkül.

**Lista állapot:** Ha van legalább egy számla, a `LazyColumn` komponens jeleníti meg őket. A `LazyColumn` a RecyclerView Compose-os megfelelője: lusta renderelést alkalmaz, azaz csak a képernyőn éppen látható elemeket rendereli le, a többi kihagyja. Ez nagy számlaállomány esetén is folyékony görgetési élményt biztosít, mert a memóriaigény és a CPU-terhelés lineárisan nő az elemszámmal, nem arányosan a teljes listával.

### 7.4 Számlakártya részletesen

Minden számlát egy `Card` komponens jelenít meg, amelyen belül az elrendezés két fő sorból áll:

**Felső sor:** Horizontálisan két kolumnára oszlik. Bal oldalon megjelenik a vevő neve (`FontWeight.Bold`, `18sp`), alatta kisebb méretben a számlasorszám (másodlagos szín) és az eladó neve (halvány szín). Jobb oldalon a bruttó összeg szerepel ugyanolyan mérettel, de elsődleges (primary) színnel kiemelve, alatta a szerkesztés (ceruza) és törlés (kuka) ikon gombok jelennek meg, amelyek mérete `32dp` és az ikonok belül `18dp` méretűek – ez kellően kicsi ahhoz, hogy ne zavarja a kártya kompaktságát, de kellően nagy ahhoz, hogy kényelmes legyen megérinteni.

**Alsó sor:** Balra a számlakiállítás dátuma `yyyy.MM.dd` formátumban és a fizetési mód látható. Jobbra, ha van csatolmány, egy `TextButton` jelenik meg a fájl nevével és egy gemkapocs ikonnal. Ez a gomb megérinthető: `Intent.ACTION_VIEW` intentet indít az eltárolt presigned URL-lel, amely a rendszer alapértelmezett alkalmazásában nyitja meg a fájlt.

### 7.5 Törlési megerősítés dialógus

A törlés gomb nem hajt végre azonnal műveletet – ehelyett egy `AlertDialog` jelenik meg, amely megkérdezi a felhasználót: „Are you sure you want to delete invoice INV-XXXX?" A számla sorszámát beleillesztem a kérdésbe, így egyértelmű, hogy melyik számla törléséről van szó. A dialógus két gombja: a piros szövegű „Delete" (hibajelző szín, jelezve a destruktív műveletet) és a „Cancel". Ez a UX-minta megakadályozza a véletlen törléseket.

### 7.6 Számla létrehozása és szerkesztése – AddInvoiceScreen

Ez az alkalmazás leghosszabb és legösszetettebb composable-je. Mivel a tartalom hosszabb a képernyőnél, a teljes oszlop görgethetővé van téve: `Column(modifier = Modifier.verticalScroll(rememberScrollState()))`. Ez az egyszerű scroll állapot feleslegessé teszi bármilyen saját görgetési logika implementálását.

**Szerkesztési és létrehozási mód:** Az `AddInvoiceScreen` egyetlen composable látja el mindkét feladatot. Az `invoiceToEdit: Invoice?` paraméter vezérli a viselkedést: ha `null`, minden beviteli mező alapértelmezett értékkel indul, és a mentés új dokumentumot hoz létre; ha nem `null`, az összes mező a meglévő számla adataival van előtöltve, és a mentés az adott dokumentumot frissíti. A fejléc szövege is ennek megfelelően változik: „New Detailed Invoice" vs „Edit Invoice".

**Számla fejléc szekció:** Egy `Card` keretbe foglalva tartalmaz egy Invoice Number szövegmezőt (amelynek alapértelmezett értéke automatikusan generált: `INV-${System.currentTimeMillis() % 10000}`, így egyedi sorszámot kap minden új számla), valamint két `ExposedDropdownMenuBox` komponenst (pénznem és fizetési mód). Az `ExposedDropdownMenuBox` a Material 3 komponense, amely egy szövegmező + legördülő lista kombinációt valósít meg. A `readOnly = true` beállítás megakadályozza, hogy a felhasználó kézzel írjon bele, csak a listából választhat.

**Eladói adatok szekciója:** Három egymás alatti `OutlinedTextField`: eladó neve, eladó címe, eladó adószáma. A szöveges mezők `Modifier.fillMaxWidth()` kiterjesztéssel az elérhető teljes szélességet elfoglalják. Az eladói adatok részben előre kitöltöttek egy alapértelmezett cégnévvel és címmel, amit a felhasználó felülírhat.

**Vevői adatok szekciója:** A fejlécben zárójelben megjelenik a „VEVŐ" jelölés is, ezzel egyértelműsítve a szekció tartalmát. Ugyanolyan három mező: vevő neve, vevő címe, vevő adószáma.

**Melléklet szekció:** Egy `Button` nyitja meg a rendszer fájlválasztóját. A gomb megnyomásakor egy `GetContent` típusú `ActivityResultLauncher` hívódik meg `"*/*"` MIME-típussal, ami bármilyen fájl kiválasztását engedélyezi. A kiválasztott fájl nevét a `ContentResolver` `DISPLAY_NAME` oszlopából olvasom ki, és megjelenítem a felhasználónak. Ha van kiválasztott fájl, megjelenik egy törlés gomb is, amellyel a felhasználó eltávolíthatja a mellékletet a mentés előtt.

**Tételek szekciója:** Két egymás melletti szövegmező (leírás és ár) és egy `AddCircle` ikon gomb alkotja a tételfelviteli sort. Az ár beviteli mezőnek `KeyboardType.Decimal` billentyűzete van beállítva, így mobilon automatikusan a numerikus billentyűzet jelenik meg decimális bevitelhez. A gomb megnyomásakor a következő számítás fut le:

```kotlin
val price = itemPrice.toDoubleOrNull()
if (itemDescription.isNotBlank() && price != null) {
    val vat = price * 0.27
    itemsList.add(InvoiceLineItem(
        description = itemDescription,
        quantity = 1.0,
        unitPrice = price,
        netPrice = price,
        vatAmount = vat,
        grossPrice = price + vat
    ))
    itemDescription = ""
    itemPrice = ""
}
```

A `toDoubleOrNull()` biztonságos konverzióval kezeli azt az esetet, ha a felhasználó nem érvényes számot írt be – ebben az esetben az `if` feltétel `false` lesz és nem kerül sor a hozzáadásra. Sikeres hozzáadás után mindkét beviteli mező ürül, készen a következő tétel felvitelére.

A hozzáadott tételek egy `mutableStateListOf<InvoiceLineItem>` listában gyűlnek, amely Compose-kompatibilis mutálható lista: a tételek hozzáadásakor és törlésekor a Compose automatikusan újrarajzolja a listát, nincs szükség manuális `notifyDataSetChanged` hívásra.

**Összesítő kártya:** A form alján a tételek összesítői automatikusan frissülnek:

```kotlin
val totalNet = itemsList.sumOf { it.netPrice }
val totalVat = itemsList.sumOf { it.vatAmount }
val totalGross = totalNet + totalVat
```

Ez a kód valós időben fut minden rekomponáláskor, ezért az összesítő azonnal frissül, ha egy tételt hozzáadnak vagy eltávolítanak. A kártyán három sor jelenik meg: „Total Net", „Total VAT (27%)" és vízszintes elválasztó után „Total Gross" (félkövérrel és elsődleges színnel kiemelve).

**Mentési folyamat és validáció:** A Mentés gomb megnyomásakor először lokális validáció fut: szükséges, hogy legyen vevő neve és legalább egy tétel. Ha a validáció sikertelen, `Toast` üzenet jelenik meg. Ha sikeres, `isSaving = true` értékre állítódik, amely letiltja a gombot és `CircularProgressIndicator`-t jelenít meg rajta. Az aszinkron mentési folyamat után – sikertől vagy hibától függetlenül – `isSaving = false`-ra áll vissza a gomb.

### 7.7 Profil képernyő

A `ProfileScreen` a bejelentkezett felhasználó adatait jeleníti meg. A Firebase `currentUser` objektumból olvasom ki a nevet (`displayName`) és az e-mail-címet (`email`). Ha a felhasználó Google-fiókkal jelentkezett be, ezek a mezők automatikusan ki vannak töltve a Google-fiók adataival; e-mail/jelszó autentikáció esetén a displayName üres lehet.

A számlák száma egy `primaryContainer` színű kártyában jelenik meg, ahol a szám `displayMedium` stílusú, amely a Material 3 tipográfia egyik legnagyobb, legkiemelkedőbb stílusa – vizuálisan hangsúlyozza ezt az értéket.

A kijelentkezés gomb tudatosan különböző stílusú (`OutlinedButton` piros kerettel és piros szöveggel), mint az alkalmazás többi gomja. A hibajelző (error) szín konvencióját a destruktív vagy visszafordíthatatlan műveleteknél alkalmazzák a Material Design irányelvek szerint – a kijelentkezés ide sorolható, mivel törli a munkamenetet.

---

## 8. Fájlkezelés és objektumtárolás

### 8.1 A MinIO választásának indokolása

Az objektumtárolás megvalósításakor az alábbi lehetőségeket vizsgáltam:

- **Firebase Storage:** Egyszerű integráció, de az ingyenes szinten korlátozott tárhely és le/feltöltési kvóta van, ráadásul a presigned URL-ek generálása bonyolultabb (Cloud Functions szükséges hozzá).
- **AWS S3:** Natív presigned URL-támogatással, de fizetős szolgáltatás fejlesztési fázisban is.
- **MinIO:** Nyílt forrású, S3-kompatibilis, lokálisan futtatható. Nem generál költséget, és az AWS S3 SDK-val teljesen kompatibilis, tehát az itt szerzett tudás közvetlen átvihetővé válik éles S3-ra is.

A MinIO választása lehetővé tette, hogy az S3 protokoll működését, a presigned URL-ek mechanizmusát és a bucket-kezelést valódi körülmények között ismerjem meg anélkül, hogy felhőköltséget kellett volna vállalnom.

### 8.2 A MinioManager megvalósítása

A `MinioManager` singleton objektum lazy inicializálású S3 klienssel működik. A lazy delegált biztosítja, hogy az S3 kliens objektum csak akkor jön létre, amikor először szükség van rá – ez javítja az alkalmazás indulási sebességét:

```kotlin
private val s3Client: AmazonS3Client by lazy {
    val credentials = BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)
    val client = AmazonS3Client(credentials, Region.getRegion(Regions.US_EAST_1))
    client.setEndpoint(ENDPOINT)
    client.setS3ClientOptions(
        S3ClientOptions.builder().setPathStyleAccess(true).build()
    )
    client
}
```

A `Region.getRegion(Regions.US_EAST_1)` beállítása technikai szükségszerűség: az AWS SDK megköveteli egy régió megadását, de a MinIO esetén ennek nincs valódi jelentősége, mivel az endpoint közvetlenül felülírja a végpont URL-jét. A `setPathStyleAccess(true)` beállítás kritikus: MinIO nem támogatja a virtual-hosted-style URL-formátumot (`http://bucket.host/object`), kizárólag a path-style formátumot (`http://host/bucket/object`).

### 8.3 Fájlfeltöltés lépésről lépésre

Az `uploadFile` suspend függvény az IO szálkészleten fut (`Dispatchers.IO`), hogy elkerülje a főszál (UI szál) blokkolását. A teljes folyamat:

**1. Fájlnév kinyerése:** Az Android fájlrendszer Content Provider alapú URI-kat (`content://...`) használ, amelyek közvetlenül nem érhetők el fájl path-ként. A fájl nevét a `ContentResolver.query` segítségével olvasom ki az `OpenableColumns.DISPLAY_NAME` oszlopból. Ha ez nem elérhető (pl. egyedi URI séma esetén), a path utolsó szegmensét használom fallback-ként.

**2. Ideiglenes fájl másolása:** Az S3 SDK közvetlen Stream feltöltése bonyolultabb; ehelyett a fájlt az alkalmazás privát gyorsítótár-könyvtárába (`context.cacheDir`) másolom:

```kotlin
private fun copyUriToTempFile(uri: Uri, context: Context, fileName: String): File {
    val tempFile = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}
```

A `use` blokkok biztosítják, hogy a stream-ek automatikusan bezáródjanak, még kivétel esetén is.

**3. Bucket-ellenőrzés:** Mielőtt feltöltöm a fájlt, ellenőrzöm, hogy az `invoices` bucket létezik-e. Ha nem, automatikusan létrehozom. Ez azért szükséges, mert egy frissen telepített MinIO szerveren nincsenek előre létrehozott bucketek.

**4. Feltöltés:** A `PutObjectRequest` tartalmazza a bucket nevét, az objektumkulcsot (a fájlnevet) és a forrás fájl referenciáját. Az S3 kliens a tényleges HTTP PUT kérést hajtja végre.

**5. Presigned URL generálása:** A feltöltés után 7 napos érvényességű aláírt URL-t generálok:

```kotlin
val expiration = Date()
var msec = expiration.time
msec += 1000L * 60 * 60 * 24 * 7
expiration.time = msec
val presignedUrl = s3Client.generatePresignedUrl(BUCKET_NAME, fileName, expiration)
```

Ez az URL az HMAC-SHA256 algoritmussal aláírt, és tartalmazza az érvényességi időt, a bucket nevét és az objektumkulcsot. A MinIO szerver az URL-ben kódolt paraméterek alapján ellenőrzi a hozzáférési jogosultságot, így az objektum elérhető anélkül, hogy a bucket publikus lenne.

**6. Takarítás:** A `finally` blokk garantálja, hogy az ideiglenes fájl mindig törlésre kerül, sikerestől és sikertelentől függetlenül:

```kotlin
} finally {
    tempFile.delete()
}
```

### 8.4 A presigned URL biztonsági modellje

A presigned URL mechanizmus egy elegáns megoldás a privát bucket tartalmához való kontrollált hozzáférésre. Az URL aláírása HMAC-SHA256 algoritmussal történik, a titkos kulcs (`SECRET_KEY`) felhasználásával. Az URL tartalmazza az érvényességi időt, és a MinIO szerver minden kérésnél ellenőrzi mind az aláírás helyességét, mind az érvényességi időt. Lejárat után az URL érvénytelenné válik, és az objektum nem érhető el rajta keresztül.

Ez a modell biztosítja, hogy:
- A bucket tartalma nem böngészhető nyilvánosan.
- Egyes fájlok időkorlátozottan megoszthatók anélkül, hogy a hitelesítő adatokat kellene megosztani.
- A 7 napos érvényesség elegendő a számlák megtekintéséhez, de véges – a számla szerkesztésével friss URL generálódik.

---

## 9. Aszinkronitás és hálózatkezelés

### 9.1 Kotlin coroutine-ok alkalmazása

Az alkalmazásban az összes aszinkron művelet Kotlin coroutine-okkal valósul meg, elkerülve a callback-pokol (callback hell) és a RxJava komplexitását. A Compose integrációhoz `rememberCoroutineScope()` segítségével kapom meg a coroutine scope-ot, amely kötve van a composable életciklusához – ha a composable eltávolításra kerül a fából (pl. navigáláskor), a scope törlődik és az összes futó coroutine is megszakad.

A fájlfeltöltés és Firestore-mentés coroutine-ban fut:

```kotlin
val scope = rememberCoroutineScope()
// ...
scope.launch {
    try {
        isSaving = true
        val attachmentUrl = selectedFileUri?.let {
            MinioManager.uploadFile(it, context)
        } ?: existingAttachmentUrl

        val invoice = Invoice(/* ... */)

        val collection = FirebaseManager.db.collection("invoices")
        val task = if (invoiceToEdit != null) {
            collection.document(invoiceToEdit.id).set(invoice.toMap())
        } else {
            collection.add(invoice.toMap())
        }

        task.addOnSuccessListener {
            isSaving = false
            onInvoiceSaved()
        }.addOnFailureListener {
            isSaving = false
            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        isSaving = false
        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
```

### 9.2 Diszpétser-rétegek

A Kotlin coroutine-ok különböző diszpétsereken futhatnak:

- **`Dispatchers.Main`** – a UI szál; composable állapotok csak erről a szálról frissíthetők helyesen
- **`Dispatchers.IO`** – hálózati és fájl I/O műveletek; a `MinioManager.uploadFile` ezen fut
- **`Dispatchers.Default`** – CPU-intenzív számítások

A `withContext(Dispatchers.IO)` hívás a `MinioManager.uploadFile` függvényen belül biztosítja, hogy a hálózati kommunikáció nem a UI szálat blokkolja. Ez különösen fontos nagy fájlok feltöltésekor, ahol a hálózati átvitel másodpercekig is eltarthat.

### 9.3 Firestore-lekérések és a kotlinx-coroutines-play-services integráció

A Firestore lekérések callback-alapú API-t nyújtanak, de a `kotlinx.coroutines.tasks.await()` kiterjesztő funkcióval ezek könnyen `suspend fun`-ná alakíthatók:

```kotlin
import kotlinx.coroutines.tasks.await
// ...
val result = FirebaseManager.db.collection("invoices").add(invoice.toMap()).await()
```

A `await()` felfüggeszti a coroutine-t, amíg a Firebase Task befejezésre kerül, majd visszaadja az eredményt vagy kivételt dob. Ez lehetővé teszi a szinkron stílusú kódírást anélkül, hogy a szálat blokkolnánk.

---

## 10. Android-specifikus megoldások

### 10.1 Storage Access Framework (SAF)

A fájlhozzáféréshez az Android modern **Storage Access Framework** (SAF) rendszerét alkalmazom. Az `ActivityResultContracts.GetContent()` kontraktus egy rendszermegjelenítő fájlbéngészt nyit meg, amelyen keresztül a felhasználó bármilyen fájlforrásból kiválaszthat fájlt (belső tároló, Google Drive, e-mail melléklet, Dropbox stb.) anélkül, hogy az alkalmazásnak `READ_EXTERNAL_STORAGE` jogosultságot kellene kérnie.

A SAF által visszaadott URI `content://` sémájú. Ezek az URI-k nem fájlrendszer-útvonalak, hanem Content Provider azonosítók. Ezért a fájl nevét a `ContentResolver` query-jével kell kinyerni:

```kotlin
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri: Uri? ->
    selectedFileUri = uri
    uri?.let {
        val cursor = context.contentResolver.query(it, null, null, null, null)
        cursor?.use { c ->
            val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst()) {
                selectedFileName = c.getString(nameIndex)
            }
        }
    }
}
```

### 10.2 Edge-to-Edge megjelenítés

Az `enableEdgeToEdge()` hívás az Android 15 alapértelmezett viselkedését aktiválja: az alkalmazás tartalma kiterjed a státuszsor és a navigációs sáv mögé is. A `Scaffold` komponens `innerPadding` paramétere gondoskodik arról, hogy a tényleges tartalom ne kerüljön rendszersávok mögé. Ez a megközelítés modern, immerzív megjelenést biztosít, és megfelel a legújabb Google Design irányelveknek.

### 10.3 Dátumformázás és lokalizáció

A számlák dátumait `Long` milliszekundum értékként tárolom a Firestore-ban. Ez platform-független és zónaváltásokra érzéketlen megoldás. A megjelenítésnél `SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())` alakítja olvashatóvá:

```kotlin
SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(invoice.date))
```

A `Locale.getDefault()` biztosítja, hogy a dátumformázás illeszkedjen a felhasználó eszközének területi beállításához. A `yyyy.MM.dd` minta szándékosan pont-elválasztókat alkalmaz, ami a magyar dátumírásmódnak megfelelő.

### 10.4 Szám formázás

Az összegek megjelenítéséhez `String.format(Locale.US, "%,.2f", összeg)` hívást alkalmazok. A `Locale.US` explicit megadása biztosítja, hogy az ezerdes elválasztó mindig vessző legyen (pl. `1,234.56`), nem pedig a rendszer lokáljától függő karakter. A `%,.2f` formátumkód vesszős ezres csoportosítást és pontosan két tizedesjegyet ad.

### 10.5 AndroidManifest konfiguráció

Az alkalmazás az alábbi jogosultságokat igényli:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Az `INTERNET` jogosultság szükséges a Firebase és a MinIO hálózati kommunikációhoz. Az `ACCESS_NETWORK_STATE` lehetővé teszi a hálózati állapot lekérdezését. Az `android:usesCleartextTraffic="true"` beállítás az Activity tagen szükséges a lokális MinIO HTTP (nem HTTPS) kommunikációhoz – éles környezetben ezt le kell cserélni HTTPS kommunikációra.

---

## 11. Állapotkezelés

### 11.1 Az állapotkezelés alapelvei Compose-ban

A Jetpack Compose-ban az állapot (state) az az adat, amelynek megváltozásakor a UI újrarajzolódik. Fontos megérteni, hogy a Compose-os állapot nem automatikusan perzisztálódik – a composable-ök újraalkotásakor (recomposition) az állapot megmarad, de az Activity vagy a process újraalkotásakor (konfiguráció-változás, pl. képernyőforgatás, vagy process-halál) elveszhet.

### 11.2 remember és rememberSaveable

A `remember` blokkba helyezett értékek a recomposition-ök között megmaradnak, de nem élnek túl konfigurációváltást. A `rememberSaveable` a `Bundle`-be menti az értéket, így képernyőforgatás után is visszaáll. Az alkalmazásban a következő stratégiát alkalmaztam:

- `rememberSaveable` – navigációs célállomás (`currentDestination`), az `isAddingInvoice` flag: ezeknek meg kell maradniuk képernyőforgatás után.
- `remember` – a `invoices` lista, az `editingInvoice` referencia, a form beviteli mezők értékei: ezek elvesztése képernyőforgatáskor elfogadható, mert a Firestore listener azonnal újra feltölti a listát, a form pedig egy üres/előtöltött állapotba tér vissza.
- `mutableStateListOf` – a tételek listája a számlafelviteli formban: ez Compose-kompatibilis, reaktív lista, amelynek módosítása automatikusan triggereli az újrarajzolást.

### 11.3 Állapot-lifting (State Hoisting)

Az alkalmazás állapot-lifting mintát alkalmaz: az állapot mindig a legmagasabb szükséges szinten él, és lefelé csak az értékei és az esemény-callbackek terjednek. Például az `invoices` lista és az `isLoading` flag a `MainScreen`-ben él, és le van adva az `InvoiceListScreen`-nek és a `ProfileScreen`-nek – azok nem tartanak fenn saját adatbázis-kapcsolatot, csak megjelenítik a kapott adatokat. Ez megakadályozza az adatok duplikálódását és az inkonzisztens állapotokat.

### 11.4 LaunchedEffect és SideEffect

A `LaunchedEffect(userId)` blokk pontosan egyszer fut le az adott kulcsértékre (`userId`) – amikor a composable először megjelenik, vagy amikor a kulcs megváltozik. A Firestore snapshot listener ebbe a blokkba kerül, mert ez garantálja, hogy ne indítson feleslegesen több párhuzamos listenert.

---

## 12. Biztonsági megfontolások

### 12.1 Adatszeparáció

Minden számladokumentum tartalmaz egy `userId` mezőt, és a Firestore lekérések mindig `whereEqualTo("userId", userId)` szűrővel futnak. Ez garantálja, hogy a felhasználó csak a saját adatait látja. Egy teljes biztonsági megoldáshoz Firestore Security Rules is szükségesek lennének (amelyek szerver oldalon is érvényesítik ezt a szabályt), de a jelenlegi implementáció kliens oldali szűrést alkalmaz.

### 12.2 Hitelesítő adatok kezelése

A MinIO hitelesítő adatok (`minioadmin`/`minioadmin`) jelenleg konstansként vannak a forráskódban. Ez fejlesztési célra elfogadható, de éles környezetben ezeket environment változókból, titkosított config-fájlból vagy Android Keystore-ból kellene beolvasni. A Firebase hitelesítő adatai (google-services.json) a `.gitignore`-nak kellene tartalmaznia, hogy ne kerüljenek verziókezelőbe.

### 12.3 Hálózati biztonság

Az alkalmazás jelenleg HTTP-t használ a MinIO felé (`android:usesCleartextTraffic="true"`). Ez fejlesztési és emulátoros kontextusban elfogadható, de éles alkalmazásban kizárólag HTTPS kommunikáció engedélyezhető. A Firebase kommunikáció mindig HTTPS-en keresztül zajlik (a Firebase SDK nem enged HTTP-t).

---

## 13. Teljesítmény-optimalizálás

### 13.1 Lusta renderelés LazyColumn-nal

A számlalista `LazyColumn` komponenssel van implementálva, amely csak a képernyőn éppen látható elemeket rendereli le. Ha a felhasználónak 500 számlája van, csak az aktuálisan látható ~5-7 kártya kerül a memóriába és a GPU-ra – a többi virtuálisan létezik. Ez az optimalizáció különösen fontos mobilon, ahol a memória és a GPU kapacitás korlátolt.

### 13.2 Lazy inicializáció

Az S3 kliens `by lazy` delegált segítségével csak az első szükség esetén jön létre. Ez javítja az alkalmazás indulási sebességét, mivel az `AmazonS3Client` konstruktora hálózati konfigurációt is elvégez.

### 13.3 Firestore offline cache

A Firestore SDK alapértelmezetten fenntart egy lokális cache-t, amely offline is elérhetővé teszi a korábban betöltött adatokat. Ez azt jelenti, hogy ha a felhasználó korábban már betöltötte a számlalistát, és elveszíti a hálózati kapcsolatot, a lista továbbra is megjelenik (de természetesen nem frissül). Az offline írási műveletek is lehetségesek – ezek egy helyi queue-ban várakoznak, és a kapcsolat helyreállításakor szinkronizálódnak.

---

## 14. Fejlesztési kihívások és megoldások

### 14.1 Az Android Emulator és a MinIO bridgelése

Az egyik legnagyobb kihívást a MinIO elérhetővé tétele jelentette az Android Emulatorból. A `localhost` és a `127.0.0.1` az emulátoron belül magát az emulátort jelöli, nem a gazdagépet. A gazdagép `localhost`-ja az emulátor `10.0.2.2` IP-cíemen érhető el – ez az Android Emulator virtuális hálózati bridge-e. Ennek megismerése és alkalmazása időt vett igénybe, de dokumentálva van az Android fejlesztői dokumentációban.

### 14.2 Path-style vs. virtual-hosted-style S3 URL

Az AWS SDK alapértelmezetten virtual-hosted-style URL-eket generál (`http://invoices.10.0.2.2:9000/fajl.pdf`), amelyeket a MinIO nem fogad el. A megoldás a `S3ClientOptions.builder().setPathStyleAccess(true).build()` beállítás explicit alkalmazása, amely path-style URL-ekre (`http://10.0.2.2:9000/invoices/fajl.pdf`) vált. Ennek kiderítéséhez az AWS SDK forráskódját és a MinIO dokumentációját kellett megvizsgálni.

### 14.3 Firestore dokumentum ID és adatosztály

A Firestore dokumentumok azonosítója (`id`) nem tárolódik a dokumentum mezőiként, csak a `DocumentSnapshot.id` property-ként érhető el. A Kotlin `data class` deszerlializálásnál a `toObject(Invoice::class.java)` nem tölti fel az `id` mezőt, ezért szükséges a `copy(id = doc.id)` hívás. Ez egy közismert pattern, de első találkozáskor zavaró lehet.

### 14.4 Google Sign-In és a Credential Manager API

Az AndroidX Credentials API viszonylag új, és a dokumentációja még fejlődik. Az `autoSelect = true` beállítással visszatérő felhasználóknál automatikusan kiválasztódik a fiók, de ez a viselkedés emulátoron és valódi eszközön eltérő lehet, mivel az emulátorban más fiókkezelési rendszer működik. A megoldás tesztelése valódi eszközön pontosabb képet ad a végfelhasználói élményről.

### 14.5 Compose rekomponálás és teljesítmény

A Compose deklaratív paradigmájában a függvény többször is meghívódhat (rekomponálás), ezért figyelni kell arra, hogy a drága műveletek (hálózati hívások, adatbázis-lekérdezések) ne kerüljenek közvetlenül composable függvények törzsébe, hanem `LaunchedEffect`, `rememberCoroutineScope` vagy állapotváltozók mögé legyenek elrejtve.

---

## 15. Technológiai összefoglaló

### 15.1 Függőségek teljes listája

| Kategória | Könyvtár | Verzió |
|---|---|---|
| UI alap | Jetpack Compose BOM | 2025.12.00 |
| UI komponensek | Material Design 3 | (BOM) |
| Adaptív navigáció | Material3 Adaptive Navigation Suite | (BOM) |
| Compose integráció | Activity Compose | 1.13.0 |
| Ikonok | Material Icons Extended | 1.7.8 |
| Firebase alap | Firebase BOM | 34.12.0 |
| Autentikáció | Firebase Authentication | (BOM) |
| Adatbázis | Cloud Firestore | (BOM) |
| Fájltárolás (Firebase) | Firebase Storage | (BOM, inicializálva) |
| Google Identity | Google Identity Library | 1.2.0 |
| Credential Manager | AndroidX Credentials API | 1.6.0 |
| Play Services Auth | Google Play Services Auth | (transitív) |
| Objektumtárolás | AWS Android SDK S3 | 2.81.1 |
| Lifecycle | AndroidX Lifecycle Runtime KTX | 2.10.0 |
| Core | AndroidX Core KTX | 1.18.0 |
| Tesztelés (unit) | JUnit | 4.13.2 |
| Tesztelés (UI) | Espresso | 3.7.0 |
| Tesztelés (Compose) | Compose UI Testing | (BOM) |

### 15.2 Build konfiguráció összefoglaló

| Beállítás | Érték |
|---|---|
| Programozási nyelv | Kotlin 2.2.10 (K2 compiler) |
| Build rendszer | Gradle 9.2.0 (AGP) |
| Compile SDK | 36 |
| Target SDK | 35 (Android 15) |
| Min SDK | 24 (Android 7.0) |
| Compose Compiler Plugin | 2.2.10 |
| Gradle Version Catalog | libs.versions.toml |

---

## 16. Tesztelés

### 16.1 Manuális tesztelési forgatókönyvek

Az alkalmazást Android Studio beépített emulátoron (Pixel 8, API 35) teszteltem. A következő felhasználói forgatókönyveket ellenőriztem szisztematikusan:

**Autentikáció:**
- Új felhasználó sikeres regisztrációja e-mail + jelszóval
- Regisztráció helytelen, nem egyező jelszavakkal (hibaüzenet megjelenik)
- Bejelentkezés helyes hitelesítő adatokkal
- Bejelentkezés helytelen jelszóval (Firebase hibaüzenet megjelenik)
- Google Sign-In folyamat (emulátori Google-fiókkal)
- Kijelentkezés és visszanavigálás az AuthScreen-re
- Alkalmazás újraindítása bejelentkezett állapotban (munkamenet megmarad)

**Számla CRUD:**
- Új számla létrehozása minden kötelező mezővel
- Új számla létrehozása fájlmelléklettel (kép és PDF is tesztelve)
- Számla szerkesztése (adatok visszatöltésének ellenőrzése, mentés után frissülés)
- Számla törlése megerősítő dialógussal
- Törlés megszakítása a Cancel gombbal
- Több tétel hozzáadása és törlése a felviteli formban
- ÁFA-számítás helyességének ellenőrzése manuálisan (nettó × 1,27 = bruttó)

**Adatszinkronizáció:**
- Számla létrehozása, majd az alkalmazás újraindítása (adatok megmaradnak)
- Valós idejű szinkronizáció ellenőrzése: számla törlése Firestore konzolon, a lista automatikusan frissül

**Fájlkezelés:**
- Képfájl kiválasztása és csatolása számlához
- PDF fájl kiválasztása és csatolása számlához
- Csatolt fájl megnyitása a listanézetből
- Melléklet eltávolítása a felviteli formban

**Navigáció és elrendezés:**
- Navigáció a Számlák és Profil képernyők között
- Képernyőforgatás a számlalistán (lista megmarad)
- Képernyőforgatás a felviteli formon (forma adatai visszaállnak)
- FloatingActionButton megjelenése és eltűnése navigáció alapján

### 16.2 Automatizált tesztek

Az alkalmazáshoz az Android project generálta alap unit és instrumentált tesztek rendelkezésre állnak (`test` és `androidTest` könyvtárakban). Ezek jelenleg az általános Compose boilerplate teszteket tartalmazzák. Az üzleti logika (ÁFA-számítás, dátumkonverzió) unit tesztelése hasznos következő lépés lenne.

---

## 17. Továbbfejlesztési lehetőségek

### 17.1 PDF számlagenerálás

Az egyik leghasznosabb fejlesztés a számlák PDF formátumban való exportálása lenne. Az Android platformon erre az `iTextPDF` vagy a `PdfDocument` (Android beépített) könyvtár alkalmas. A generált PDF tartalmazhatná a számlafejlécet, az eladói és vevői adatokat, a tétellistát táblázatban, az összesítő sorokat, sőt akár az eladó logóját is. Ez a funkció közvetlenül megoszthatóvá tenné a számlát e-mailben vagy üzenetküldő alkalmazásban.

### 17.2 Offline támogatás megerősítése

A Firestore SDK offline cache-e ugyan működik, de az alkalmazás jelenleg nem kezeli explicit módon a hálózati állapot változásait. Egy `ConnectivityManager.NetworkCallback` beillesztésével a felhasználót értesíteni lehetne, ha offline módban van, és a módosításai várakoznak a szinkronizálásra.

### 17.3 Keresés és szűrés

A számlalista jelenleg nem kereshető és nem szűrhető. Egy keresősáv hozzáadása (a fejlécben elhelyezett `SearchBar` Material 3 komponenssel) lehetővé tenné a vevő neve, számlasorszám vagy összeg alapján való keresést. A szűrés dátumtartomány, pénznem vagy fizetési mód alapján is hasznos lenne.

### 17.4 Statisztikák és riportok

Egy dedikált statisztika képernyő hozzáadásával a felhasználó összesített képet kaphatna: havi bevétel oszlopdiagramon, legtöbb számlát kiállított vevők ranglistája, fizetési módok megoszlása. Ezek mind kiszámíthatók kliens oldalon a betöltött számlalista alapján.

### 17.5 Push értesítések

A Firebase Cloud Messaging (FCM) integrálásával a fizetési határidőhöz közelítő számlákról értesítést küldhetne az alkalmazás. Ez megvalósítható egy Firebase Cloud Function segítségével, amely naponta fut, megvizsgálja a lejáró határidőket, és FCM push értesítést küld az érintett felhasználóknak.

### 17.6 HTTPS és éles MinIO

Éles környezetben a MinIO-t HTTPS-sel kellene futtatni (Let's Encrypt tanúsítvánnyal vagy vállalati CA-val), és az alkalmazásból el kellene távolítani a `usesCleartextTraffic` beállítást. Alternatívaként az AWS S3-ra való áttérés szintén lehetséges, mivel a `MinioManager` teljes egészében S3-kompatibilis API-t használ – csak az endpoint URL-t kellene módosítani.

### 17.7 Beviteli validáció megerősítése

A jelenlegi validáció minimális (vevő neve és legalább egy tétel szükséges). Éles alkalmazásban szükség lenne a következőkre:
- Adószám formátum ellenőrzése (magyarországi adószám: 8-1-2 szegmenses formátum)
- Számlasorszám egyediségének ellenőrzése
- Bankszámlaszám formátum validáció
- Kötelező mezők vizuális jelölése és validációs hibaüzenetek a mezők alatt

---

## 18. Összefoglalás

Az Invoice Store Android alkalmazás fejlesztése során egy komplex, többrétegű rendszert hoztam létre, amely egyesíti a modern mobilfejlesztés legjobb gyakorlatait: a Jetpack Compose deklaratív UI-paradigmáját, a Firebase felhőszolgáltatások erejét, és az S3-kompatibilis objektumtárolás rugalmasságát.

A projekt elvégzése révén mélyreható tapasztalatot szereztem a Kotlin coroutine-ok aszinkron programozásban betöltött szerepéről, az egyirányú adatfolyam és az állapot-lifting Compose-os megvalósításáról, a Firebase Authentication különböző módszereiről és a munkamenet-kezelés mechanizmusáról, az S3 protokoll és a presigned URL-ek biztonsági modelljéről, az Android Storage Access Framework-ről és a Content Provider URI-k kezeléséről, valamint a Material Design 3 adaptív navigációs rendszeréről.

A fejlesztés legtanulságosabb mozzanata a MinIO-integráció volt, különösen az Android Emulator bridge-elése és a path-style URL-ek szükségességének megértése. Ezek valódi éles fejlesztői problémák, amelyek megoldása értékes, transzferálható tudást adott.

Az elkészült alkalmazás egy stabil, funkcióteljes alap, amely érdemben megoldja a számlakezelés mobilos kihívásait. A tervezett fejlesztésekkel (PDF exportálás, push értesítések, statisztikák) egy teljes értékű, üzleti környezetben is bevethető eszközzé válhatna.
