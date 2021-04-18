
import java.io.*;
import java.util.*;

import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.trees.TreeCoreAnnotations.*;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;


//import packages
public class languageSentiment{
	public static void main(String[] args){
		HashMap<String, Long> countries = new HashMap<String, Long>();
		HashMap<String, Integer> countriesNumber = new HashMap<String, Integer>();
		HashMap<String, Long> countriesPositive = new HashMap<String, Long>();
		HashMap<String, Long> countriesNegative = new HashMap<String, Long>();
		HashMap<Integer, String> numberCountries = new HashMap<Integer, String>();
		List<HashMap<String , dictionaryEntry>> dictionary  = new ArrayList<HashMap<String,dictionaryEntry>>();

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
	// retrieve all languages, then contruct a dector object
	// to use a text factory in order to decipher language
	List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
	LanguageDetector languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
        	.withProfiles(languageProfiles)
        	.build();
	TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
	TextObject textObject = textObjectFactory.forText(in);
	Optional<LdLocale> lang = languageDetector.detect(textObject);
	if(lang.equals("en") || lang.equals("English"))
	{
		return true;
	}
	return false;
}

static String getTextFromID(long id){
	final Twitter twitter = new TwitterFactory().getInstance();
	 twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_KEY_SECRET);
	 AccessToken accessToken = new AccessToken(TWITTER_TOKEN, TWITTER_TOKEN_SECRET);
	 twitter.setOAuthAccessToken(accessToken);
	 try {
			 Status status = twitter.showStatus(id);
			 if(status != null) {
				 // successfully found tweet
					 return status.getText();
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

		// else this tweet has been marked positively
		// (since there are 5 sentiment levels 0->4, the nuetral
		// score of 0 is counted as positive
		else  {	return 1; }
	}
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
