# BgBus
### Android aplikacija koja pronalazi najkraći put između dva mesta koristeći gradski prevoz
##### Radi offline

Bazirana na [BelgradeUnderground](https://github.com/luq-0/BelgradeUnderground) zadatku i algoritmu tamo korišćenom.

## Značajne razlike

Prilagođeno za Android

Pronalazi više mogućih puteva, tako što pušta algoritam iz Paths više puta sa različitim parametrima i prikazuje i prilagođava rezultate u realnom vremenu

Funkcioniše za bilo koje dve tačke na mapi (koristi Gugl mape za odabir lokacije)

Veći dataset: obuhvata prigradske (Lastine) i noćne linije

Uzima u obzir trenutno vreme (za prve i poslednje polaske) i period polazaka


## Nedostaci

Podaci nisu ažurirani nekoliko godina - BusPlus sistem ne dozvoljava nikakav programski pristup sistemu (a red vožnje se nalazi iza captcha-e!), tako da sam morao da se snalazim kako znam i umem, i nisam pronašao nijedan lep način koji mogu lako često da repliciram.

## License

GNU GPLv3
