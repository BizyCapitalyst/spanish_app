package com.james.gramaticaconversacional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GrammarEvaluator {
    static final String TARGET = "target";
    static final String GRAMMAR = "grammar";
    static final String TRANSFER = "transfer";
    static final String NATURAL = "natural";
    static final String DIALECT = "dialect";

    static final class Correction {
        final String category;
        final String said;
        final String better;
        final String explanation;

        Correction(String category, String said, String better, String explanation) {
            this.category = category;
            this.said = said;
            this.better = better;
            this.explanation = explanation;
        }
    }

    static final class Result {
        final List<Correction> corrections = new ArrayList<>();
        boolean hasTargetCorrection() {
            for (Correction c : corrections) if (TARGET.equals(c.category)) return true;
            return false;
        }
    }

    private static final class Rule {
        final Pattern pattern;
        final String said;
        final String better;
        final String explanation;
        final String category;

        Rule(String regex, String said, String better, String explanation, String category) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            this.said = said;
            this.better = better;
            this.explanation = explanation;
            this.category = category;
        }
    }

    private static final Rule[] RULES = new Rule[] {
        new Rule("\\bla problema\\b", "la problema", "el problema", "'Problema' is masculine even though it ends in -a, so it takes the masculine article 'el'.", GRAMMAR),
        new Rule("\\buna problema\\b", "una problema", "un problema", "'Problema' is masculine, so use 'un problema'.", GRAMMAR),
        new Rule("\\bla tema\\b", "la tema", "el tema", "'Tema' is masculine even though it ends in -a, so use 'el tema'.", GRAMMAR),
        new Rule("\\buna tema\\b", "una tema", "un tema", "'Tema' is masculine, so use 'un tema'.", GRAMMAR),
        new Rule("\\bel sistema\\b", "el sistema", "el sistema", "This form is correct; no correction is needed.", NATURAL),
        new Rule("\\bla sistema\\b", "la sistema", "el sistema", "'Sistema' is masculine, so use 'el sistema'.", GRAMMAR),
        new Rule("\\bel situación\\b", "el situación", "la situación", "'Situación' is feminine, so use the feminine article 'la'.", GRAMMAR),
        new Rule("\\bun situación\\b", "un situación", "una situación", "'Situación' is feminine, so use 'una situación'.", GRAMMAR),
        new Rule("\\bun decisión\\b", "un decisión", "una decisión", "'Decisión' is feminine, so use 'una decisión'.", GRAMMAR),
        new Rule("\\bel decisión\\b", "el decisión", "la decisión", "'Decisión' is feminine, so use 'la decisión'.", GRAMMAR),
        new Rule("\\bel mano\\b", "el mano", "la mano", "'Mano' is feminine even though it ends in -o, so use 'la mano'.", GRAMMAR),
        new Rule("\\bmucho libertad\\b", "mucho libertad", "mucha libertad", "'Libertad' is feminine, so 'mucho' must agree with it: 'mucha libertad'.", GRAMMAR),
        new Rule("\\bmucho responsabilidad\\b", "mucho responsabilidad", "mucha responsabilidad", "'Responsabilidad' is feminine, so use 'mucha responsabilidad'.", GRAMMAR),
        new Rule("\\bmuchos responsabilidad\\b", "muchos responsabilidad", "muchas responsabilidades", "Both gender and number must agree: 'muchas responsabilidades'.", GRAMMAR),
        new Rule("\\bmuchas responsabilidad\\b", "muchas responsabilidad", "muchas responsabilidades", "The noun also needs to be plural: 'muchas responsabilidades'.", GRAMMAR),
        new Rule("\\bmi vida (es|era|fue|esta|estaba|se puso|se ha puesto) (mas )?tranquilo\\b", "mi vida ... tranquilo", "mi vida ... tranquila", "'Vida' is feminine, so an adjective describing it must also be feminine: 'tranquila'.", GRAMMAR),

        new Rule("\\bsi tendria\\b", "si tendría", "si tuviera", "For a hypothetical condition, Spanish uses the imperfect subjunctive after 'si': 'si tuviera', not 'si tendría'.", GRAMMAR),
        new Rule("\\bsi tendrias\\b", "si tendrías", "si tuvieras", "For a hypothetical condition, use the imperfect subjunctive after 'si': 'si tuvieras'.", GRAMMAR),
        new Rule("\\bsi tendrian\\b", "si tendrían", "si tuvieran", "For a hypothetical condition, use the imperfect subjunctive after 'si': 'si tuvieran'.", GRAMMAR),
        new Rule("\\bespero que tiene\\b", "espero que tiene", "espero que tenga", "After 'espero que', Spanish normally uses the subjunctive because you are expressing a hope: 'tenga'.", GRAMMAR),
        new Rule("\\bquiero que tiene\\b", "quiero que tiene", "quiero que tenga", "After 'quiero que' with a different subject, use the subjunctive: 'tenga'.", GRAMMAR),
        new Rule("\\bpara que tiene\\b", "para que tiene", "para que tenga", "Purpose clauses with 'para que' take the subjunctive: 'para que tenga'.", GRAMMAR),
        new Rule("\\bhe tuve\\b", "he tuve", "he tenido", "The present perfect uses 'haber + past participle': 'he tenido'.", GRAMMAR),
        new Rule("\\bhe tenia\\b", "he tenía", "he tenido", "The present perfect uses the past participle 'tenido': 'he tenido'.", GRAMMAR),

        new Rule("\\busted tienes\\b", "usted tienes", "usted tiene", "'Usted' uses third-person singular verb forms: 'usted tiene'.", GRAMMAR),
        new Rule("\\bella tienes\\b", "ella tienes", "ella tiene", "'Ella' uses third-person singular: 'ella tiene'.", GRAMMAR),
        new Rule("\\bel tienes\\b", "él tienes", "él tiene", "'Él' uses third-person singular: 'él tiene'.", GRAMMAR),
        new Rule("\\bnosotros tiene\\b", "nosotros tiene", "nosotros tenemos", "With 'nosotros', use 'tenemos'.", GRAMMAR),
        new Rule("\\bnosotras tiene\\b", "nosotras tiene", "nosotras tenemos", "With 'nosotras', use 'tenemos'.", GRAMMAR),
        new Rule("\\bustedes tiene\\b", "ustedes tiene", "ustedes tienen", "With 'ustedes', use the third-person plural form 'tienen'.", GRAMMAR),
        new Rule("\\bellos tiene\\b", "ellos tiene", "ellos tienen", "With 'ellos', use the plural form 'tienen'.", GRAMMAR),
        new Rule("\\bellas tiene\\b", "ellas tiene", "ellas tienen", "With 'ellas', use the plural form 'tienen'.", GRAMMAR),
        new Rule("\\btu tiene\\b", "tú tiene", "tú tienes", "With 'tú', use 'tienes'.", GRAMMAR),

        new Rule("\\bhace sentido\\b", "hace sentido", "tiene sentido", "This is a direct transfer from English 'makes sense'. Natural Spanish normally uses 'tener sentido': 'tiene sentido'.", TRANSFER),
        new Rule("\\bhacer sentido\\b", "hacer sentido", "tener sentido", "English says 'make sense', but Spanish normally says 'tener sentido'. Learn it as a complete Spanish expression.", TRANSFER),
        new Rule("\\bhacer una decision\\b", "hacer una decisión", "tomar una decisión", "This comes from English 'make a decision'. Spanish normally uses 'tomar una decisión'.", TRANSFER),
        new Rule("\\bhice una decision\\b", "hice una decisión", "tomé una decisión", "This reflects English 'made a decision'. In Spanish, say 'tomé una decisión'.", TRANSFER),
        new Rule("\\brealice que\\b", "realicé que", "me di cuenta de que", "Do not transfer English 'I realized' to Spanish 'realicé'. The natural expression is 'me di cuenta de que'.", TRANSFER),
        new Rule("\\brealize que\\b", "realicé que", "me di cuenta de que", "English 'realized' is normally expressed as 'me di cuenta de que', not with 'realizar'.", TRANSFER),
        new Rule("\\bdepende en\\b", "depende en", "depende de", "This often comes from English 'depends on'. Spanish uses the preposition 'de': 'depende de'.", TRANSFER),
        new Rule("\\ben orden para\\b", "en orden para", "para", "This is a calque of English 'in order to'. Conversational Spanish normally just uses 'para'.", TRANSFER),
        new Rule("\\bcorrer un negocio\\b", "correr un negocio", "dirigir un negocio", "English 'run a business' does not normally transfer as 'correr un negocio'. Use 'dirigir', 'manejar' or 'llevar' a business depending on context.", TRANSFER),
        new Rule("\\bla razon es porque\\b", "la razón es porque", "la razón es que", "English strongly favors 'the reason is because'. In Spanish, 'la razón es que...' is usually cleaner and more natural.", TRANSFER),

        new Rule("\\byo pienso que yo\\b", "yo pienso que yo...", "creo que...", "Spanish usually omits subject pronouns when the verb already identifies the person. 'Creo que...' is more natural conversational structure unless you need contrast or emphasis.", NATURAL),
        new Rule("\\byo creo que yo\\b", "yo creo que yo...", "creo que...", "Repeated explicit 'yo' often reflects English sentence structure. Spanish normally leaves the subject implicit when it is clear.", NATURAL),
        new Rule("\\ben mi opinion personal\\b", "en mi opinión personal", "en mi opinión", "'Personal' is usually redundant here. 'En mi opinión' is the more natural conversational phrase.", NATURAL),
        new Rule("\\bpara mi personalmente\\b", "para mí personalmente", "para mí", "Spanish usually does not need both 'para mí' and 'personalmente' unless you are emphasizing a contrast.", NATURAL),

        new Rule("\\bvosotros\\b", "vosotros", "ustedes", "Your target is Latin American Spanish. Use 'ustedes' for plural 'you'; we are intentionally not training 'vosotros' right now.", DIALECT),
        new Rule("\\bteneis\\b", "tenéis", "tienen", "'Tenéis' is a vosotros form. In your Latin American target dialect, use 'ustedes tienen'.", DIALECT),
        new Rule("\\btendreis\\b", "tendréis", "tendrán", "'Tendréis' is a vosotros form. In Latin American Spanish, use 'ustedes tendrán'.", DIALECT)
    };

    static Result evaluate(String answer, TutorContent.Prompt prompt) {
        Result result = new Result();
        String n = norm(answer);

        for (Rule rule : RULES) {
            if (result.corrections.size() >= 5) break;
            if (rule.said.equals(rule.better)) continue;
            if (rule.pattern.matcher(n).find()) {
                addUnique(result, new Correction(rule.category, rule.said, rule.better, rule.explanation));
            }
        }

        if (prompt != null && !containsAny(n, prompt.expected)) {
            String wrong = firstContained(n, prompt.contrasts);
            if (wrong != null) {
                addUnique(result, new Correction(TARGET, wrong, prompt.expected[0], prompt.explanation));
            }
        }

        return result;
    }

    private static void addUnique(Result result, Correction correction) {
        for (Correction existing : result.corrections) {
            if (existing.said.equalsIgnoreCase(correction.said) && existing.better.equalsIgnoreCase(correction.better)) return;
        }
        result.corrections.add(correction);
    }

    private static boolean containsAny(String normalizedAnswer, String[] phrases) {
        for (String p : phrases) if (containsPhrase(normalizedAnswer, norm(p))) return true;
        return false;
    }

    private static String firstContained(String normalizedAnswer, String[] phrases) {
        for (String p : phrases) if (containsPhrase(normalizedAnswer, norm(p))) return p;
        return null;
    }

    private static boolean containsPhrase(String text, String phrase) {
        if (phrase.length() == 0) return false;
        Pattern p = Pattern.compile("(^|\\s)" + Pattern.quote(phrase) + "($|\\s)");
        return p.matcher(text).find() || text.contains(phrase);
    }

    static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(new Locale("es")), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return n.replace('ñ', 'n')
            .replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private GrammarEvaluator() {}
}
