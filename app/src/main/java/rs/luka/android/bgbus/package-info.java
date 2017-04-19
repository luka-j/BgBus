/**
 * Htedoh da napišem par reči o kodu ovde, ali pošto je sve što sam planirao da uradim trajalo 2-3 puta duže
 * nego planirano, nemam previše vremena.
 * Dokumentacija je.. so-so. Ona 'novijeg' datuma je na engleskom, stariji deo je na srpskom (sem ovoga,
 * ovo pišem malo pre predavanja projekta). Ideju sam dobio nakon testa za nedelju informatike (neka predavanja
 * u školi, nebitno), u kom je jedan od zadataka bio "Evo vam 3 json fajla s podacima o stajalištima i linijama,
 * treba naći najkraći put od stanice A do stanice B". Za mesec dana sam to proširio na "Nađi sve što možeš
 * od podataka i sklopi algoritam koji neće izazivati primetna zastajkivanja u radu aplikacije koji će da nađe
 * najkraći put koristeći sav mogući kontekst koji može da dobije na jednom mobilnom telefonu".
 * Nisam došao ni do pola niti smatram da je to moguće za mesec dana uz školske obaveze. Podaci su preuzimani
 * sa sajta GSPa i GitHuba (open source projekti sa sličnom tematikom). Možda je neki deo i sa busevi.com,
 * mada mi se čini da sam to izbacio, jer nisam uspeo da ih upotrebim (stoje mi u lokalnoj bazi na kompu).
 * Cilj aplikacije nije neka preterano inovativna ideja (put od tačke A do tačke B za ljude bez vozačke, gde
 * ćete prostije?), već sam akcenat hteo da stavim na praktičnost i realizaciju. Nešto najbliže ovome je PlanPlus
 * za čiju aplikaciju vidim rastući broj slabih ocena na Playu, pa mi ovo liči kao solidna prilika. Pritom, postoje
 * opcije da se uzima trenutno vreme u obzir (koje mi se čini da u nekim situacijama baguje, ali nisam stigao
 * da proverim sve), noćne, prigradske i ADA linije, što bi trebalo da nadmaši sve što trenutno postoji za Beograd,
 * pod uslovom da radi kako treba, naravno.
 * Ima dosta stvari koje se ne koriste (dead code) i iskomentarisanih delova. Pošto je ovo i dalje debug build,
 * a ja nemam viška vremena, ostavljam ih. Aplikacija nije ni izbliza temeljno testirana i možda je baš neki
 * od iskomentarisanih delova uzrok nekog bug-a koji nisam uspeo da pronađem.
 * Sve ostale informacije se nalaze u dokumentaciji, dok su neki detalji raštrkani po komentarima. Da napomenem,
 * ako neko i dalje nije shvatio, komentari predstavljaju moje lično mišljenje i njihov sadržaj u velikom
 * zavisi od mog raspoloženja u tom trenutku. Dokumentacija je uglavnom malčice ozbiljnija. Malčice.
 *
 * Bilo je lepo iskustvo, jedino mi je žao što nisam imao više vremena. Tokom letnjeg raspusta sam radio drugu
 * aplikaciju, koju je kao trebalo da prijavim s timom. Bilo je više zainteresovanih iz mog odeljenja; niko od
 * njih nije znao ništa sem C/C++ (proceduralni deo), ali sam im poslao neke knjige i rekao da pitaju ako šta
 * treba i ukratko objasnio osnove. Do septembra, ostao je jedan dečko, koji je trebalo da radi server-side.
 * Fast-forward pre sedam dana, nakon ne znam koliko puta ponavljanja da nema teorije da stigne da uradi sve,
 * šalje mi jedan fajl sa desetak skripti i stotinak kompajlerskih grešaka. Veliki deo je iskopiran, ali to mi
 * nije bilo toliko bitno (rekao sam mu da koristi šta hoće, kako hoće i da pita ako mu nešto nije jasno).
 * Sad imam aplikaciju od 13ak hiljada redova koda na disku bez odgovarajućeg back-enda. Video sam da će tako
 * da se završi i pre slanja prijava, zato sam i prijavio ovu samostalno, čisto da imam nešto. Da se razumemo
 * taj deo sa php skriptama i bazom je apsurdno kratak u poređenju s onim što sam već uradio i stignem sam da
 * ga završim za jedan vikend. Neću iz principa. Ako se desi neko čudo pa jedne nedelje ne budem imao nijedno
 * ispitivanje i nijedan kontrolni/pismeni, lako ću ja to dovršiti i objaviti pod svojim imenom na Play-u.
 * "Ako hoćeš da uradiš nešto kako treba, uradi to sam." As simple as that. Ne znam ko je sve na kraju od
 * drugaka iz MG predao (videću u svakom slučaju), jer ja ove moje što su odustali ili nisu stigli u potpunosti
 * razumem; potrebno je mnogo vremena i volje, a pritom se ne preklapa sa školskim gradivom ni u jednoj tački,
 * pa ovo treba raditi paralelno sa učenjem industrijskih i laboratorijskih dobijanja pola periodnog sistema i
 * sličnih debilizama (u 'debilizme' ne računam fiziku i matematike, iako ne vidim veliku korist matematike
 * na tom nivou za pravljenje consumer apps. Ali šta ja znam, imam jedva tri iz analize, možda mi nekad i bude
 * korisna). Videti rant iznad {@link rs.luka.android.bgbus.model.Line#Line(java.lang.String)}.
 *
 * Hope you liked the story. Sad još samo treba napisati opis nekim zvaničnijim tonom.
 * See you!
 */

package rs.luka.android.bgbus;


//One day, someone will kill me for embedding stories in comments and especially docs. Or because of them. Oh well.
//No regrets, they make this much more interesting.