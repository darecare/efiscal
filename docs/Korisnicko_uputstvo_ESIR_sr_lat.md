# Korisničko uputstvo za eFiscal (srpski latinica)

Verzija dokumenta: 0.1 (radna implementacija)
Datum: 2026-07-10

## 1. Svrha dokumenta

Ovo uputstvo opisuje funkcionalnosti koje su trenutno implementirane u aplikaciji eFiscal u ovom codespace okruženju.

Dokument je podeljen na:
- operativni korisnički deo (kako se aplikacija koristi),
- ESIR mapiranje (šta je podržano, delimično podržano ili zahteva dodatnu verifikaciju).

Važna napomena:
- Ovaj dokument nije sertifikaciona izjava.
- Statusi u ESIR tabeli su procena na osnovu implementacije (UI + API), bez tvrdnje o formalnoj regulatornoj verifikaciji.

## 2. Pristup aplikaciji

- Frontend URL: `http://localhost:5173`
- API bazna putanja: `/api/v1`
- Razvojni nalog:
  - Email: `admin@efiscal.local`
  - Lozinka: `Admin123!`

### 2.1 Prijava

1. Otvorite Login stranicu.
2. Unesite email i lozinku.
3. Kliknite na dugme za prijavu.

Mesto za screenshot: `SS-01 Login ekran`

## 3. Globalna navigacija i osnovni koncepti

Posle prijave aplikacija koristi:
- bočnu navigaciju po modulima,
- izbor aktivne organizacije u zaglavlju,
- izbor jezika (sr/en),
- About modal (proizvođač, serijski broj, verzija softvera),
- kontrolu pristupa po akcijama (RBAC).

### 3.1 Aktivna organizacija

Veliki broj operativnih stranica zahteva izabranu aktivnu organizaciju. Bez toga su akcije onemogućene.

Mesto za screenshot: `SS-02 Header + izbor organizacije`

### 3.2 About informacije (ESIR identitet softvera)

U meniju pomoći dostupne su informacije:
- proizvođač,
- serijski broj,
- verzija softvera.

Mesto za screenshot: `SS-03 About modal`

## 4. Porudžbine (Orders)

Svrha:
- preuzimanje MerchantPro porudžbina po filterima,
- pokretanje fiskalizacije iz porudžbine.

Koraci:
1. Izaberite aktivnu organizaciju.
2. Podesite filtere (datum/status/limit).
3. Kliknite „Preuzmi porudžbine“.
4. Po potrebi pokrenite izdavanje fiskalnog računa iz izabrane porudžbine.

Napomene:
- Tok koristi idempotency ključ za kreiranje računa.
- Fiskalizacija iz porudžbine šalje stavke i podatke kupca u backend.

Mesto za screenshot: `SS-04 Orders lista`

## 5. Ručno kreiranje fiskalnog računa

Stranica: `Fiskalni računi > Kreiraj`

Podržano:
- ručni unos zaglavlja i stavki,
- automatski obračun stavke: `quantity * unitPrice` sa zaokruživanjem na 2 decimale,
- pretraga proizvoda po nazivu (uz SKU/EAN predloge),
- live lookup cene po SKU/EAN,
- GTIN/EAN podrška na stavci,
- više načina plaćanja na jednom računu (split payment),
- opcionalno slanje email-a kupcu,
- validacije i prikaz rezultata,
- PDF preuzimanje nakon uspeha.

Koraci:
1. Izaberite organizaciju.
2. Unesite tip računa i tip transakcije.
3. Dodajte stavke (ručno ili izborom iz kataloga).
4. Unesite načine plaćanja (jedan ili više).
5. Po potrebi unesite referentni broj dokumenta.
6. Kliknite na izdavanje.

Mesto za screenshot: `SS-05 Create Fiscal Bill`

## 6. Fiskalni računi (pregled, detalji, copy/refund, PDF/HTML)

Stranica: `Fiskalni računi`

Podržano:
- listanje računa po organizaciji,
- klijentski filteri (datum, broj računa, orderId, tip računa, tip transakcije, kupac),
- pregled detalja poreza i plaćanja,
- preuzimanje PDF (A4 i Roll80),
- HTML preview računa,
- kreiranje Copy računa,
- kreiranje Refund računa,
- retry neuspešnih fiskalizacija.

Mesto za screenshot:
- `SS-06 Fiscal Bills lista`
- `SS-07 Fiscal Bill detalji + PDF/HTML`

## 7. Status fiskalizacije i poreske stope

Stranica: `Fiskalni računi > Status`

Podržano:
- čitanje statusa od poreskog servisa za aktivnu organizaciju,
- prikaz tekućih i istorijskih grupa poreskih stopa,
- prikaz kategorija i oznaka stopa.

Mesto za screenshot: `SS-08 Get Status`

## 8. Proizvodi

Stranica: `Fiskalni računi > Proizvodi`

Podržano:
- CRUD kataloga proizvoda,
- pretraga,
- bulk akcije,
- soft-delete,
- sinhronizacija iz shop sistema (AUTO / RESET_FULL),
- prikaz/provera statusa sync procesa,
- otkazivanje sync procesa.

Mesto za screenshot: `SS-09 Products`

## 9. Administracija

### 9.1 Korisnici
- lista i upravljanje korisnicima (u skladu sa dodeljenim akcijama).

Mesto za screenshot: `SS-10 Users`

### 9.2 Uloge i dozvole
- kreiranje/izmena uloga,
- dodela akcija ulozi,
- zaštita sistemskih uloga.

Mesto za screenshot: `SS-11 Roles`

### 9.3 Organizacije i klijenti
- upravljanje organizacijama,
- upravljanje klijentima (superadmin domen).

Mesto za screenshot:
- `SS-12 Organizations`
- `SS-13 Clients`

## 10. Podešavanja

### 10.1 API konfiguracija
- konekcije ka spoljnim servisima,
- endpoint šabloni po operaciji,
- auth tipovi i tehnička podešavanja.

Mesto za screenshot: `SS-14 API Config`

### 10.2 Email templejti
- org-scope templejti,
- subject + HTML body,
- aktivacija/deaktivacija templejta.

Mesto za screenshot: `SS-15 Email Templates`

### 10.3 Mapiranje tipa plaćanja
- mapiranje spoljnog paymentMethodCode na fiskalni paymentType (0..6).

Mesto za screenshot: `SS-16 PayType Map`

### 10.4 Porezi
- lokalno održavanje poreskih kategorija i stopa,
- import stopa iz status endpoint-a.

Mesto za screenshot: `SS-17 Taxes`

## 11. ESIR mapiranje (na osnovu pitanja iz fajla „Pitanja za esir prijavu.md“)

Legenda statusa:
- Podržano
- Delimično podržano
- Nije pronađeno u implementaciji
- Zahteva runtime/verifikacionu potvrdu

| ESIR stavka (sažeto) | Status | Napomena |
|---|---|---|
| Vrste/tipovi računa po klasifikaciji ESIR | Delimično podržano | Implementirani su invoiceType i transactionType, ali mapiranje 1:1 na sve ESIR klasifikacije zahteva verifikaciju. |
| Ne menja obavezne podatke primljene od PFR | Zahteva runtime/verifikacionu potvrdu | Potrebna analiza stvarnih PFR odgovora i izlaza računa. |
| Ne menja zaglavlje podataka izdavaoca primljenih od PFR | Zahteva runtime/verifikacionu potvrdu | Potreban dokaz na stvarnim fiskalizovanim primerima. |
| Izbacivanje stavki pre izdavanja | Podržano | Ručni tok omogućava dodavanje/uklanjanje stavki pre slanja. |
| Popust na stavku | Delimično podržano | Nema posebno „discount“ polje; moguće je korigovati cenu/ukupan iznos stavke. |
| Proizvođač, serijski broj, verzija softvera lako dostupni | Podržano | About modal + `/api/v1/app-info`. |
| Registracija svih payment type (0..6) | Podržano | Frontend/backend podržavaju 0..6 i mapiranje. |
| Režim ograničavanja određenih načina plaćanja | Delimično podržano | Org payment types mogu ograničiti ponudu; specifično regulatorno ponašanje treba potvrditi test scenarijem. |
| Višestruki način plaćanja istog računa | Podržano | Manual create podržava više payment redova. |
| Izdavanje u elektronskom obliku ili štampa | Delimično podržano | PDF/HTML i email su podržani; direktna štampa zavisi od korisničkog okruženja. |
| GTIN podrška | Podržano | GTIN/EAN podržan u proizvodu i stavci računa. |
| Refundacija/kopija traži referentni broj | Delimično podržano | Postoje copy/refund tokovi; način unosa/prikaza referenci treba potvrditi na izlazu računa. |
| Prodaja iz avansa/predračuna sa referentnim brojem | Delimično podržano | Referentni broj postoji u ručnom toku sa uslovnim prikazom. |
| HTTPS komunikacija sa V-PFR | Zahteva runtime/verifikacionu potvrdu | Tehnički predviđeno kroz konfiguraciju konekcija; potrebno potvrditi deployment konfiguracijom. |
| Elektronski žurnal svih računa sa pretragom | Podržano | Fiscal Bills lista + filteri + detalji. |
| Unos transakcije preko fajla | Nije pronađeno u implementaciji | Nije detektovan UI/API tok za import stavki iz lokalnog fajla. |
| Novi proizvod/usluga se može uneti od korisnika | Podržano | Products modul ima create/update. |
| Izbor količine tokom izdavanja računa | Podržano | Količina je obavezno polje stavke. |
| Promena cene proizvoda/usluge | Podržano | Ručni unos cene i korekcija stavke su podržani. |
| Zaokruživanje PLU vrednosti na 2 decimale | Podržano | Obračun stavke koristi toFixed(2). |
| Biranje artikla po nazivu ili GTIN skeniranjem | Delimično podržano | Pretraga po nazivu i EAN postoji; skeniranje kao posebna funkcija nije potvrđeno. |
| Uvoz/izvoz liste proizvoda i usluga | Delimično podržano | Podržan je sync (uvoz) iz shop-a; izvoz liste nije potvrđen. |
| Preuzimanje poreskih stopa iz konfiguracije PFR/SUF | Delimično podržano | Get Status i import u Taxes postoje; potrebno potvrditi punu proizvodnu konfiguraciju izvora. |
| Prikaz poreske oznake i vrednosti na računu | Zahteva runtime/verifikacionu potvrdu | Potreban pregled generisanog fiskalnog dokumenta iz realnog scenarija. |
| Podrška za aktuelne poreske oznake + proširenje | Podržano | Taxes/Categories modul omogućava održavanje oznaka i stopa. |
| Prikaz poreskih stopa na zahtev | Podržano | Get Status ekran prikazuje stope. |
| Zaokruživanje poreske vrednosti na 2 decimale | Zahteva runtime/verifikacionu potvrdu | Potrebna potvrda na izlaznom računu i backend obračunu poreza. |
| Ne koristi druge stope osim L-PFR/V-PFR | Zahteva runtime/verifikacionu potvrdu | Potreban audit poslovnih pravila i runtime tokova. |
| Formati štampe (57mm/80mm/A4/drugo) | Delimično podržano | A4 i Roll80 PDF/HTML su eksplicitno podržani. |
| Podržani štampači (eksterni/ugrađeni/drugo) | Nije pronađeno u implementaciji | Nema direktne driver-level funkcije štampe u aplikaciji. |
| Dostavljanje računa (papir/elektronski) | Delimično podržano | Elektronski tok (email/PDF/HTML) postoji; papir preko eksternog print workflow-a. |
| Tekstualni prikaz računa – zaglavlje/metapodaci/QR/ref. broj | Zahteva runtime/verifikacionu potvrdu | Potrebni primeri stvarno izdatih računa i vizuelna validacija svih obaveznih elemenata. |
| Poruka „Ovo nije fiskalni račun“ za Copy/Predračun/Obuka | Zahteva runtime/verifikacionu potvrdu | Potrebna provera template-a i izlaza po tipu dokumenta. |
| Završna linija fiskalnog dela + reklamno polje | Delimično podržano | PDF template podrška postoji, ali konkretna usklađenost zahteva verifikaciju template sadržaja. |

## 12. Ograničenja i sledeći koraci

### 12.1 Screenshot status

Screenshotovi su uzeti kroz VS Code browser chat tools, ali trenutno nisu trajno zapisani kao fajlovi u workspace putem istog alata.

Do sada su potvrđeni sledeći capture primeri u ovoj sesiji:
- Login / glavni shell (chat screenshot capture)
- Kreiranje fiskalnog računa (chat screenshot capture)
- Lista fiskalnih računa (chat screenshot capture)
- Proizvodi (chat screenshot capture)

Predlog za finalizaciju dokumenta:
1. Ponoviti capture kroz alat koji snima PNG direktno u repo (ili ručno eksportovati chat-capture slike).
2. Ubaciti slike u ovaj dokument na mesta `SS-01` do `SS-17`.
3. Zaključati verziju kao finalnu 1.0.

### 12.2 Verifikacioni paket za ESIR prijavu

Za stavke sa statusom „Zahteva runtime/verifikacionu potvrdu“, pripremiti:
- set realnih test računa (prodaja, refund, copy, avans tokovi),
- PDF/HTML izlaze,
- zapis parametara i odgovora servisa,
- checklist evidenciju po svakoj ESIR tački.
