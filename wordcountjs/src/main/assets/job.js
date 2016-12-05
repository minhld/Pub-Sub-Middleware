function entry() {
    if (window.jsInterface) {
        // get JSON string describes request from server (url, number of parts, index)
        var pageText = window.jsInterface.getFile('string');
        pageText = window.jsInterface.normalizeHtml(pageText);
        pageText = pageText + pageText + pageText;

        var topWords = findTopWords(pageText, 50);

        var json5Words = JSON.stringify(topWords);

        // return results
        window.jsInterface.returnResult(json5Words); // returnResult(btoa(json5Words));

		$('body').append('<div>' + json5Words + '</div>');
    } else {
        $('body').append('<div>No callback interface (Not on Android)</div>');
    }
}

/**
 * find top 5 words that appear the most in the document
 * @param pageText
 * @param nTop
 */
function findTopWords(pageText, nTop) {
    pageText = cleanText(pageText);
    var words = pageText.split(' ');

    var wordCounts = { };

    for (var i = 0; i < words.length; i++) {
        wordCounts["_" + words[i]] = (wordCounts["_" + words[i]] || 0) + 1;
        /*
        if (!checkMinorWords(words[i])) {
            wordCounts["_" + words[i]] = (wordCounts["_" + words[i]] || 0) + 1;
        }
        */
    }

    var keys = Object.keys(wordCounts);
    var topWords = {};

    for (var i = 0; i < nTop; i++) {
        var maxPos = 0;
        for (var j = i + 1; j < keys.length; j++) {
            if (wordCounts[keys[j]] > wordCounts[keys[maxPos]]) {
                maxPos = j;
            }
        }

        var key = keys[maxPos];
        topWords[key] = wordCounts[key];

        // this will be out of the loop
        wordCounts[key] = 0;
    }

    return topWords;
}

var stopWords = [ "i", "im", "a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "amoungst", "amount",  "an", "and", "another", "any", "anyhow", "anyone", "anything", "anyway", "anywhere", "are", "around", "as",  "at", "back", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom", "but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "did", "didnt", "do", "done", "dont", "down", "due", "during", "each", "eg", "eight", "either", "eleven", "else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own", "part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the" ];


/**
 * remove the redundant words out of the string (like the, there, etc.)
 * @param word
 */
function checkMinorWords(word) {
    for (var i = 0; i < stopWords.length; i++) {
        if (word == stopWords[i]) {
            return true;
        }
    }

    return false;
}

/**
 * clean all the redundant characters out of the string
 * @param s
 */
function cleanText(s) {
    s = s.replace(/(^\s*)|(\s*$)/gi, "").
          replace(/[ ]{2,}/gi, " ").
          replace(/\n /, "\n").
          replace(/\'/g, "").
          replace(/\"/g, "").
          replace(/,/g, "").
          toLowerCase();
    return s;
}

$(document).ready(function() {
    entry();
});
