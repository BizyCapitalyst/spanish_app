package com.james.gramaticaconversacional;

final class TutorContent {
    static final class Prompt {
        final int index;
        final String question;
        final String focus;
        final String[] expected;
        final String[] contrasts;
        final String model;
        final String explanation;

        Prompt(int index, String question, String focus, String[] expected, String[] contrasts, String model, String explanation) {
            this.index = index;
            this.question = question;
            this.focus = focus;
            this.expected = expected;
            this.contrasts = contrasts;
            this.model = model;
            this.explanation = explanation;
        }
    }

    private static String[] a(String... items) { return items; }

    static final Prompt[] PROMPTS = new Prompt[] {
        new Prompt(0,
            "Para empezar, cuéntame qué responsabilidades tienes ahora que no tenías hace cinco años.",
            "present · yo",
            a("tengo"), a("tenía", "tuve", "he tenido"),
            "Ahora tengo responsabilidades diferentes, pero también tengo más libertad.",
            "Use the present form 'tengo' for what you have now. The question deliberately contrasts your present situation with the past."),

        new Prompt(1,
            "Cuando trabajabas a tiempo completo, ¿qué responsabilidades tenías normalmente y cuáles te pesaban más?",
            "imperfect · yo",
            a("tenía"), a("tuve", "tengo", "he tenido"),
            "Tenía muchas responsabilidades y algunas me pesaban bastante.",
            "Use the imperfect 'tenía' for an ongoing or habitual background situation in the past."),

        new Prompt(2,
            "Cuéntame de una ocasión específica en la que tuviste que resolver un problema difícil. ¿Qué pasó?",
            "preterite · yo",
            a("tuve", "tuve que"), a("tenía", "tengo", "he tenido"),
            "Tuve que tomar una decisión difícil y resolver el problema rápidamente.",
            "Use the preterite 'tuve' for a specific completed situation or event."),

        new Prompt(3,
            "Mirando los últimos años, ¿qué experiencias interesantes has tenido desde que te jubilaste?",
            "present perfect · yo",
            a("he tenido"), a("tuve", "tenía", "tengo"),
            "He tenido experiencias muy distintas desde que me jubilé.",
            "Use 'he tenido' when connecting past experiences with the present period you are still living through."),

        new Prompt(4,
            "Pensando en los próximos meses, ¿qué responsabilidades crees que tendrás que asumir?",
            "future · yo",
            a("tendré", "tendré que"), a("tendría", "tengo", "tenía"),
            "Tendré que organizar mejor mi tiempo y asumir algunas responsabilidades nuevas.",
            "Use the future 'tendré' for something you expect to have or have to do later."),

        new Prompt(5,
            "Si tuvieras un año completamente libre, ¿qué cambios harías en tu vida y por qué?",
            "imperfect subjunctive · yo/tú",
            a("tuviera", "tuvieras"), a("tendría", "tendrías", "tengo", "tienes"),
            "Si tuviera un año completamente libre, dedicaría más tiempo a viajar y a conocer gente.",
            "After a hypothetical 'si' clause, Spanish uses the imperfect subjunctive: 'si tuviera/tuvieras', not the conditional."),

        new Prompt(6,
            "¿Qué tendría que cambiar en tu vida para que hablaras español con más naturalidad todos los días?",
            "conditional · él/ella/usted/idea impersonal",
            a("tendría", "tendría que"), a("tuviera", "tiene", "tendrá"),
            "Tendría que cambiar mi rutina para usar más español de forma espontánea.",
            "Use the conditional 'tendría' for what would need to happen under a hypothetical condition."),

        new Prompt(7,
            "Antes de mudarte a Panamá, ¿ya habías tenido experiencias viviendo fuera de Estados Unidos? Cuéntame.",
            "past perfect · yo",
            a("había tenido"), a("tuve", "tenía", "he tenido"),
            "Antes de mudarme a Panamá, ya había tenido algunas experiencias viviendo fuera.",
            "Use 'había tenido' for something that had already happened before another past event."),

        new Prompt(8,
            "Piensa en una mujer cercana a ti. ¿Qué esperas que ella tenga en su vida durante los próximos años?",
            "present subjunctive · ella",
            a("tenga"), a("tiene", "tendrá", "tenía"),
            "Espero que ella tenga estabilidad, buenos amigos y tiempo para disfrutar la vida.",
            "After expressions of hope such as 'espero que', Spanish normally uses the subjunctive: 'que ella tenga'."),

        new Prompt(9,
            "Háblame de un hombre que conoces bien. ¿Qué responsabilidades tiene él actualmente?",
            "present · él",
            a("tiene"), a("tienes", "tengo", "tienen"),
            "Él tiene varias responsabilidades profesionales y familiares.",
            "With 'él', the present form is 'tiene'."),

        new Prompt(10,
            "Imagina que aconsejas formalmente a un profesional panameño. Empieza con 'Usted' y dile qué tiene que tener en cuenta antes de tomar una decisión importante.",
            "present · usted",
            a("usted tiene", "tiene que tener", "tiene"), a("tú tienes", "tienes que", "tenéis"),
            "Usted tiene que tener en cuenta el riesgo, el tiempo y las consecuencias de la decisión.",
            "'Usted' uses third-person verb forms: 'usted tiene', not 'usted tienes'."),

        new Prompt(11,
            "Si tú y yo organizáramos un proyecto juntos, ¿qué responsabilidades tendríamos nosotros?",
            "conditional · nosotros",
            a("tendríamos", "nosotros tendríamos"), a("tendrían", "tendría", "tendríamos vosotros"),
            "Tendríamos que dividir las responsabilidades y mantener una comunicación clara.",
            "With 'nosotros', the conditional form is 'tendríamos'."),

        new Prompt(12,
            "Imagina que hablas con varios amigos. Diles qué tendrán ustedes que considerar antes de mudarse a otro país.",
            "future · ustedes",
            a("ustedes tendrán", "tendrán"), a("vosotros tendréis", "tendréis", "tienen"),
            "Ustedes tendrán que considerar el costo de vida, la cultura y la red social que quieren construir.",
            "In Latin American Spanish, plural 'you' is 'ustedes' and takes third-person plural forms such as 'tendrán'."),

        new Prompt(13,
            "Piensa en dos personas que conoces. Si ellos tuvieran más tiempo libre, ¿qué crees que harían?",
            "imperfect subjunctive · ellos",
            a("tuvieran", "ellos tuvieran"), a("tendrían", "tienen", "tenían"),
            "Si ellos tuvieran más tiempo libre, probablemente viajarían más.",
            "In a hypothetical 'si' clause with 'ellos', use the imperfect subjunctive 'tuvieran'."),

        new Prompt(14,
            "Cuéntame de una ocasión en la que ellos tuvieron que adaptarse rápidamente a un cambio.",
            "preterite · ellos",
            a("tuvieron", "ellos tuvieron"), a("tenían", "tienen", "tendrían"),
            "Ellos tuvieron que adaptarse rápidamente porque la situación cambió de repente.",
            "Use 'tuvieron' for a completed event involving 'ellos/ellas/ustedes'."),

        new Prompt(15,
            "Imagina que tú y yo estamos organizando una cena. ¿Qué tenemos que hacer nosotros antes de que lleguen los invitados?",
            "present · nosotros",
            a("tenemos", "tenemos que"), a("tienen", "tengo", "tenéis"),
            "Tenemos que organizar la comida y tener todo listo antes de que lleguen.",
            "With 'nosotros', the present form is 'tenemos'."),

        new Prompt(16,
            "Ahora hazme una pregunta sobre mis planes de mañana usando 'tú' y 'tener que'.",
            "present · tú",
            a("tienes que", "tú tienes"), a("tiene que", "tenéis que", "tengo que"),
            "¿Qué tienes que hacer mañana?",
            "With 'tú', the present form is 'tienes'. In your Latin American target dialect, use 'tú' rather than 'vosotros'."),

        new Prompt(17,
            "Piensa en una etapa pasada de tu vida en pareja. Habla de los dos usando 'nosotros': ¿qué responsabilidades tenían juntos?",
            "imperfect · nosotros",
            a("teníamos", "nosotros teníamos"), a("tenían", "tenemos", "tuvimos"),
            "Nosotros teníamos responsabilidades distintas, pero compartíamos las decisiones importantes.",
            "With 'nosotros' in the imperfect, use 'teníamos'. Use the imperfect for recurring background responsibilities."),

        new Prompt(18,
            "Para cerrar esta vuelta, dime qué cosas importantes quieres tener en cuenta cuando hablas español con otras personas.",
            "expression · tener en cuenta",
            a("tener en cuenta", "tengo en cuenta", "quiero tener en cuenta"), a("hacer en cuenta", "tomar en cuenta"),
            "Quiero tener en cuenta la naturalidad, el contexto y la manera en que realmente habla la gente.",
            "'Tener en cuenta' is a common conversational expression meaning 'to keep in mind' or 'take into account'.")
    };

    private TutorContent() {}
}
