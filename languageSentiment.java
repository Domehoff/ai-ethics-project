
import java.io.*;
import java.util.*;

import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.http.AccessToken;


//import packages
public class languageSentiment{
	private static final String CONSUMER_KEY = "LNuTahzBiJlCXkJ2ACdas16rQ";
	private static final String CONSUMER_KEY_SECRET = "UGZrrUtA16jY36BsemIFaLQN0TlK5Sj3gNuWfvfbypG0RUIknp";
	private static final String TWITTER_TOKEN = "3192975130nJWZLKk6gjZEt6oWzHNnLa5JsYsdmV9ZhzFpNBV";
	private static final String TWITTER_TOKEN_SECRET = "64So33HEWkjw9F9x5yRgcbp7is7YaAIxpe9O50Agysarx";

	public static void main(String[] args){
		HashMap<String, Long> countries = new HashMap<String, Long>();
		HashMap<String, Integer> countriesNumber = new HashMap<String, Integer>();
		HashMap<String, Long> countriesPositive = new HashMap<String, Long>();
		HashMap<String, Long> countriesNegative = new HashMap<String, Long>();
		HashMap<Integer, String> numberCountries = new HashMap<Integer, String>();
		List<HashMap<String , dictionaryEntry>> dictionary  = new ArrayList<HashMap<String,dictionaryEntry>>();
		//System.out.println("Got here");
		Scanner reader;
		try {
			reader = new Scanner(new File("geo_2020-04-21.json"));

		String in = "";
		long id = -1;
		String text = "";
		String countryCode = "";
		int sentiment = -1;
		int countryIncrement = 0;
		boolean flag = true;
		String temp;

			//CHANGE THE TIME TO GET MORE DATA, PARSING THE WHOLE FILE WILL TAKE A LONG TIME
		double maxTime = 10000;

		double currentTime = System.currentTimeMillis();

		while(flag = true){
			in = "  ";
			while(!in.substring(in.length()-2,in.length()).equals("]}")) {
				temp = reader.next();
				if(!reader.hasNext()) {
					System.out.println("null");
					flag = false;
					break;
				}
				in = in + temp;
			}
			if(!flag || (System.currentTimeMillis()-currentTime>maxTime))
				break;
			if(isTagged(in)) {
				id = getIDFromLine(in);
				text = getTextFromID(id);



				if(textIsEnglish(text)){
					countryCode = getCountryFromLine(in);
					if(!countries.containsKey(countryCode)) {
						countries.put(countryCode, (long) 0);
						countriesNumber.put(countryCode,countryIncrement);
						numberCountries.put(countryIncrement, countryCode);
						dictionary.add(new HashMap<String,dictionaryEntry>());
						countryIncrement++;
					}
					countries.put(countryCode, countries.get(countryCode)+1);
					sentiment = generateSentiment(text);



					String[] splitText = text.split("\\W");
					for(int i = 0; i < splitText.length; i++) {
						if(splitText[i].length()>0) {
							if(dictionary.get(countriesNumber.get(countryCode)).get(splitText[i])==null){
								dictionary.get(countriesNumber.get(countryCode)).put(splitText[i], new dictionaryEntry(splitText[i]));
							}
							(dictionary.get(countriesNumber.get(countryCode))).get(splitText[i]).inc(sentiment==1);
						}
					}
				}
			}

		}
		//i will make it output to file
		System.out.println(Arrays.asList(countries));
		for(int i = 0; i < countryIncrement; i++) {
			HashMap thisMap = dictionary.get(i);
			List<HashMap> thisCountryDictionary = Arrays.asList(thisMap);
			for(int j = 0; j < thisCountryDictionary.size();  j++) {
				System.out.println(numberCountries.get(i) + " : " + thisCountryDictionary.get(j));
			}
		}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

 }

static String cleanTweet(String in) {
  in = in.trim()
  // remove unnecessary characters and elements
  .replaceAll("http.*?[\\S]+", "")
  .replaceAll("@[\\S]+", "")
  .replaceAll("#", "")
  .replaceAll("[\\s]+", " ");
   return in;
}

static long getIDFromLine(String in){
	return Long.parseLong(in.substring(15, in.indexOf("\",\"created_at")));
}


static boolean textIsEnglish(String in) {
	// retrieve all languages
	// to use a text factory in order to decipher language
	LanguageDetector identifier = new OptimaizeLangDetector().loadModels();
	identifier.reset();
    identifier.addText(in);
    LanguageResult language = identifier.detect();
    if(language.getLanguage().equals("en") || language.getLanguage().equals("english")) 
    { return true; }
	return false;
}

static String getTextFromID(long id){
	final ConfigurationBuilder cb = new ConfigurationBuilder()
		      .setOAuthConsumerKey(CONSUMER_KEY)
		      .setOAuthConsumerSecret(CONSUMER_KEY_SECRET)
		      .setOAuthAccessToken(TWITTER_TOKEN)
		      .setOAuthAccessTokenSecret(TWITTER_TOKEN_SECRET);
	final Twitter twitter = new TwitterFactory(cb.build()).getInstance();
	 try {
			 Status status = twitter.showStatus(id);
			 if(status != null) {
				 // successfully found tweet
					 return cleanTweet(status.getText());
			 }
	 } catch (TwitterException e) {
			 System.err.print("Failed to search tweets: " + e.getMessage());
	 }
	 // if status was null
	 return "";

}

static boolean isTagged(String in) {
	return in.contains("country_code");
}

static String getCountryFromLine(String in){
	return in.substring(in.indexOf("country_code\":\"")+15, in.indexOf("country_code\":\"")+17);
}

static int generateSentiment(String text){
	//FILL IN
	//1 if positive, 0 if negative
	Properties props = new Properties();
	props.setProperty("annotators", "tokenize, ssplit, pos, parse, sentiment");

	StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	Annotation annotation = pipeline.process(text);

	for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
		Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
		int temp = RNNCoreAnnotations.getPredictedClass(tree);

		// if the tweet is scored 0 or 1, this is a negatively
		// marked sentiment
		if(temp == 0 || temp == 1) {
			return 0;
		}
		// disregard text
		else if(temp == 2) {
			return -1;
		}
		// else this tweet has been marked positively
		else  {	return 1; }
	}
	return 0;
}

}
class dictionaryEntry {
	String word;
	double positive;
	double negative;
	double total;
	double percentage;
	public dictionaryEntry(String w){
		word = w;
		positive = 0;
		negative = 0;
		total = 0;
	}
	public void inc(boolean s) {
		if(s)
			positive++;
		else
			negative++;
		total++;
	}
	public String toString() {
		return "Word " + word + " has " + positive + " positive hits, " + negative + " negative hits, " + total + " total hits ";
	}
}
