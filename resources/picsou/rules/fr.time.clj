(
  ;; generic
  
  "two time tokens in a row"
  [(dim :time #(not (:latent %))) (dim :time #(not (:latent %)))] ; sequence of two tokens with a time dimension
  (intersect %1 %2)

  ; same thing, with "de" in between like "mardi de la semaine dernière"
  "two time tokens separated by `de`"
  [(dim :time #(not (:latent %))) #"(?i)de" (dim :time #(not (:latent %)))] ; sequence of two tokens with a time fn
  (intersect %1 %3)
  
   ;;;;;;;;;;;;;;;;;;;
  ;; Named things

  "named-day"
  #"(?i)lundi|lun|lun\."
  (day-of-week 1)

  "named-day"
  #"(?i)mardi|mar|mar\."
  (day-of-week 2)

  "named-day"
  #"(?i)mercredi|mer|mer\."
  (day-of-week 3)

  "named-day"
  #"(?i)jeudi|jeu|jeu\."
  (day-of-week 4)

  "named-day"
  #"(?i)vendredi|ven|ven\."
  (day-of-week 5)

  "named-day"
  #"(?i)samedi|sam|sam\."
  (day-of-week 6)

  "named-day"
  #"(?i)dimanche|dim|dim\."
  (day-of-week 7)

  "named-month"
  #"(?i)janvier|janv\.?"
  (month 1)

  "named-month"
  #"(?i)fevrier|février|fev|fév\.?"
  (month 2)

  "named-month"
  #"(?i)mars|mar\.?"
  (month 3)

  "named-month"
  #"(?i)avril|avr\.?"
  (month 4)

  "named-month"
  #"(?i)mai"
  (month 5)

  "named-month"
  #"(?i)juin|jun\.?"
  (month 6)

  "named-month"
  #"(?i)juillet|juil?\."
  (month 7)

  "named-month"
  #"(?i)aout|août|aou\.?"
  (month 8)

  "named-month"
  #"(?i)septembre|sept?\.?"
  (month 9)

  "named-month"
  #"(?i)octobre|oct\.?"
  (month 10)

  "named-month"
  #"(?i)novembre|nov\.?"
  (month 11)

  "named-month"
  #"(?i)décembre|decembre|déc\.?|dec\.?"
  (month 12)

  "maintenant"
  #"maintenant|(tout de suite)"
  (cycle-nth :second 0)
  
  "aujourd'hui"
  #"(?i)(aujourd'? ?hui)|(ce jour)|(dans la journ[ée]e?)|(en ce moment)"
  (cycle-nth :day 0)

  "demain"
  #"(?i)demain"
  (cycle-nth :day 1)

  "hier"
  #"(?i)hier"
  (cycle-nth :day -1)

  "après-demain"
  #"(?i)apr(e|è)s[- ]?demain"
  (cycle-nth :day 2)

  "avant-hier"
  #"(?i)avant[- ]?hier"
  (cycle-nth :day -2)

  ;;
  ;; This, Next, Last


  "ce <day-of-week>" ; assumed to be in the future "ce dimanche"
  [#"(?i)ce" {:form :day-of-week}]
  (pred-nth-not-immediate %2 0)

  ;; for other preds, it can be immediate:
  ;; "ce mois" => now is part of it
  ; See also: cycles in en.cycles.clj
  "ce <time>"
  [#"(?i)ce" (dim :time)]
  (pred-nth %2 0)

  "<named-month|named-day> prochain"
  [(dim :time) #"(?i)prochain"]
  (pred-nth %1 1)

  "<named-month|named-day> dernier|passé"
  [(dim :time) #"(?i)dernier|pass[ée]e?"]
  (pred-nth %1 -1)

  "<named-day> en huit" ; would need assumption to handle 1 or 2 weeks depending on the day-of-week
  [{:form :day-of-week} #"(?i)en (huit|8)"]
  (pred-nth %1 1)

  "<named-day> en quinze" ; would need assumption to handle 2 or 3 weeks depending on the day-of-week
  [{:form :day-of-week} #"(?i)en (quinze|15)"]
  (pred-nth %1 2)

  ; Years
  ; Between 1000 and 2100 we assume it's a year
  ; Outside of this, it's safer to consider it's latent
  
  "year (1000-2100 not latent)"
  (integer 1000 2100)
  (year (:val %1))

  "year (latent)"
  (integer -10000 999)
  (assoc (year (:val %1)) :latent true)

  "year (latent)"
  (integer 2101 10000)
  (assoc (year (:val %1)) :latent true)

  ; Day of month appears in the following context:
  ; - le premier
  ; - le 5
  ; - 5 March
  ; - mm/dd (and other numerical formats like yyyy-mm-dd etc.)
  ; We remove the rule with just (integer 1 31) as it was too messy

  "day of month (premier)"
  [#"(?i)premier|prem\.?|1er|1 er"]
  (day-of-month 1)

  "le <day-of-month> (non ordinal)" ; this one is latent
  [#"(?i)le" (integer 1 31)]
  (assoc (day-of-month (:val %2)) :latent true)
  
  "<day-of-month> <named-month>" ; 12 mars
  [(integer 1 31) {:form :month}]
  (intersect %2 (day-of-month (:val %1)))




  ;; hours and minutes (absolute time)
  "<integer> (latent time-of-day)"
  (integer 0 23)
  (assoc (hour (:val %1) true) :latent true)

  "<time-of-day> heures"
  [{:form :time-of-day}  #"(?i)h\.?(eure)?s?"]
  (dissoc %1 :latent) 
  
  "à|vers <time-of-day>" ; absorption
  [#"(?i)[aà]|vers" {:form :time-of-day}]
  (dissoc %2 :latent) 

  "hh(:|h)mm (time-of-day)"
  #"(?i)((?:[01]?\d)|(?:2[0-3]))[:h]([0-5]\d)"
  (hour-minute (Integer/parseInt (first (:groups %1)))
               (Integer/parseInt (second (:groups %1)))
               true)
  
  "hhmm (military time-of-day)"
  #"(?i)((?:[01]?\d)|(?:2[0-3]))([0-5]\d)"
  (-> (hour-minute (Integer/parseInt (first (:groups %1)))
                (Integer/parseInt (second (:groups %1)))
                false) ; not a 12-hour clock
      (assoc :latent true))
    
  "midi"
  #"(?i)midi"
  (-> (hour 12 false)
      (assoc :form :time-of-day
             :for-relative-minutes true :val 12))

  "minuit"
  #"(?i)minuit"
  (-> (hour 0 false)
      (assoc :form :time-of-day
             :for-relative-minutes true :val 0))

  "quart (relative minutes)"
  #"(?i)quart"
  {:relative-minutes 15}

  "trois quarts (relative minutes)"
  #"(?i)(3|trois) quarts?"
  {:relative-minutes 45}

  "demi (relative minutes)"
  #"demie?"
  {:relative-minutes 30}

  "number (as relative minutes)"
  (integer 1 59)
  {:relative-minutes (:val %1)}
  
  ;"<integer> minutes (as relative minutes)"; tobechecked
  ;[(integer 1 59) #"(?i)min\.?(ute)?s?"]
  ;{:relative-minutes (:val %1)}

  "<hour-of-day> <integer> (as relative minutes)"
  [(integer 0 23) #(:relative-minutes %)] ;before  [{:for-relative-minutes true} #(:relative-minutes %)]
  (hour-relativemin (:val %1) (:relative-minutes %2) true)

  "<hour-of-day> heures <integer> (as relative minutes)" ;ALEX: to manage  quinze heure quinze
  [(integer 0 23) #"(?i)h\.?(eure)?s?( et)?" #(:relative-minutes %)] ;before  [{:for-relative-minutes true} #(:relative-minutes %)]
  (hour-relativemin (:val %1) (:relative-minutes %3) true)

  ;special forms for midnight and noon
  ;for-relative-minutes is only for midnight and noon hence calling hour-relativemin with false
  "relative minutes <integer> (as relative minutes for noon midnight)"
  [#(:for-relative-minutes %) #(:relative-minutes %)]
  (hour-relativemin (:val %1) (:relative-minutes %2) false)

  ;"<hour-of-day> moins <integer> (as relative minutes)"
  ;[{:for-relative-minutes true} #"moins( le)?" #(:relative-minutes %)]
  ;(hour-relativemin 
  ;  (:val %1)
  ;  (:ambiguous-am-pm %1)
  ;  (- (:relative-minutes %3)))
  "<hour-of-day> moins <integer> (as relative minutes)"
  [(integer 0 23) #"moins( le)?" #(:relative-minutes %)]
  (hour-relativemin (:val %1) (- (:relative-minutes %3)) true)

  "<hour-of-day> et|passé de <relative minutes>"
  [(integer 0 23) #"et|(pass[ée]e? de)" #(:relative-minutes %)]
  (hour-relativemin (:val %1) (:relative-minutes %3) true)
  
  ; special forms for midnight and noon
  ;for-relative-minutes is only for midnight and noon hence calling hour-relativemin with false
  "relative minutes (noon or midnight) et|passé de <relative minutes>"
  [#(:for-relative-minutes %) #"et|(pass[ée]e? de)" #(:relative-minutes %)]
  (hour-relativemin (:val %1) (:relative-minutes %3) false)
  
  "relative minutes (noon or midnight) moins <integer> (as relative minutes)"
  [#(:for-relative-minutes %) #"moins( le)?" #(:relative-minutes %)]
  (hour-relativemin (:val %1) (- (:relative-minutes %3)) false)

  ;; Formatted dates and times

  "dd/mm/yyyy"
  #"([012]?\d|30|31)/(0?\d|10|11|12)/(\d{2,4})"
  (parse-dmy (first (:groups %1)) (second (:groups %1)) (nth (:groups %1) 2) true)

  "yyyy-mm-dd"
  #"(\d{2,4})-(0?\d|10|11|12)-([012]?\d|30|31)"
  (parse-dmy (nth (:groups %1) 2) (second (:groups %1)) (first (:groups %1)) true)
  
  "dd/mm"
  #"([012]?\d|30|31)/(0?\d|10|11|12)"
  (parse-dmy (first (:groups %1)) (second (:groups %1)) nil true)
  

  ; Part of day (morning, evening...). They are intervals.

  "matin"
  #"(?i)matin[ée]?e?"
  (assoc (interval (hour 4 false) (hour 12 false) false) :form :part-of-day :latent true)

  "après-midi"
  #"(?i)apr[eéè]s?[ \-]?midi"
  (assoc (interval (hour 12 false) (hour 19 false) false) :form :part-of-day :latent true)
  
  "soir"
  #"(?i)soir[ée]?e?"
  (assoc (interval (hour 18 false) (hour 0 false) false) :form :part-of-day :latent true)
  
  "dans le <part-of-day>" ;; removes latent
  [#"(?i)dans l[ae']? ?" {:form :part-of-day}]
  (dissoc %2 :latent)
  
  "ce <part-of-day>"
  [#"(?i)cet?t?e?" {:form :part-of-day}]
  (assoc (intersect (cycle-nth :day 0) %2) :form :part-of-day) ;; removes :latent

  "<dim time> <part-of-day>" ; since "morning" "evening" etc. are latent, general time+time is blocked
  [(dim :time) {:form :part-of-day}]
  (intersect %1 %2)

  ; Other intervals: week-end, seasons
  "week-end"
  #"(?i)week(\s|-)?end"
  (interval (intersect (day-of-week 5) (hour 18 false))
            (intersect (day-of-week 1) (hour 0 false))
            false)

  "season"
  #"(?i)été" ;could be smarter and take the exact hour into account... also some years the day can change
  (interval (month-day 6 21) (month-day 9 23) false)

  "season"
  #"(?i)automne"
  (interval (month-day 9 23) (month-day 12 21) false)

  "season"
  #"(?i)hiver"
  (interval (month-day 12 21) (month-day 3 20) false)

  "season"
  #"(?i)printemps"
  (interval (month-day 3 20) (month-day 6 21) false)

  ;; Time zones
  
  ;"timezone"
  ;#"(?i)(YEKT|YEKST|YAPT|YAKT|YAKST|WT|WST|WITA|WIT|WIB|WGT|WGST|WFT|WEZ|WET|WESZ|WEST|WAT|WAST|VUT|VLAT|VLAST|VET|UZT|UYT|UYST|UTC|ULAT|TVT|TMT|TLT|TKT|TJT|TFT|TAHT|SST|SRT|SGT|SCT|SBT|SAST|SAMT|RET|PYT|PYST|PWT|PT|PST|PONT|PMST|PMDT|PKT|PHT|PHOT|PGT|PETT|PETST|PET|PDT|OMST|OMSST|NZST|NZDT|NUT|NST|NPT|NOVT|NOVST|NFT|NDT|NCT|MYT|MVT|MUT|MST|MSK|MSD|MMT|MHT|MEZ|MESZ|MDT|MAWT|MART|MAGT|MAGST|LINT|LHST|LHDT|KUYT|KST|KRAT|KRAST|KGT|JST|IST|IRST|IRKT|IRKST|IRDT|IOT|IDT|ICT|HOVT|HNY|HNT|HNR|HNP|HNE|HNC|HNA|HLV|HKT|HAY|HAT|HAST|HAR|HAP|HAE|HADT|HAC|HAA|GYT|GST|GMT|GILT|GFT|GET|GAMT|GALT|FNT|FKT|FKST|FJT|FJST|ET|EST|EGT|EGST|EET|EEST|EDT|ECT|EAT|EAST|EASST|DAVT|ChST|CXT|CVT|CST|COT|CLT|CLST|CKT|CHAST|CHADT|CET|CEST|CDT|CCT|CAT|CAST|BTT|BST|BRT|BRST|BOT|BNT|AZT|AZST|AZOT|AZOST|AWST|AWDT|AST|ART|AQTT|ANAT|ANAST|AMT|AMST|ALMT|AKST|AKDT|AFT|AEST|AEDT|ADT|ACST|ACDT)"
  ;{:dim :timezone
  ; :value (-> %1 :groups first .toUpperCase)}
  
  ;"<time> timezone"
  ;[(dim :time) (dim :timezone)]
  ;(assoc %1 :timezone (:value %2))

)
