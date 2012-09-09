package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.Element;

import fiji.plugin.trackmate.detection.DownsampleLogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.NearestNeighborTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.SimpleFastLAPTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;

public class TrackerProvider <T extends RealType<T> & NativeType<T>> extends AbstractProvider implements TrackerKeys {


	protected static final String XML_ELEMENT_NAME_LINKING = "Linking";
	protected static final String XML_ELEMENT_NAME_GAP_CLOSING = "GapClosing";
	protected static final String XML_ELEMENT_NAME_MERGING = "TrackMerging";
	protected static final String XML_ELEMENT_NAME_SPLITTING = "TrackSplitting";
	protected static final String XML_ELEMENT_NAME_FEATURE_PENALTIES = "FeaturePenalties";

	/** The tracker names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant tracker classes.  */
	protected List<String> keys;
	protected String currentKey = SimpleFastLAPTracker.TRACKER_KEY;
	protected String errorMessage;
	private ArrayList<String> names;
	private ArrayList<String> infoTexts;
	protected final TrackMateModel<T> model;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the spot trackers currently available in the 
	 * TrackMate plugin. Each tracker is identified by a key String, which can be used 
	 * to retrieve new instance of the tracker, settings for the target tracker and a 
	 * GUI panel able to configure these settings.
	 * <p>
	 * To proper instantiate the target {@link SpotTracker}s, this provider has a reference
	 * to the target model. It is this provider's responsibility to pass the required 
	 * info to the concrete {@link SpotTracker}, extracted from the stored model.
	 * <p>
	 * If you want to add custom trackers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom trackers and pass this 
	 * extended provider to the {@link TrackMate_} plugin.
	 */
	public TrackerProvider(TrackMateModel<T> model) {
		this.model = model;
		this.currentKey = SimpleFastLAPTracker.TRACKER_KEY;
		registerTrackers();
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard trackers shipped with TrackMate.
	 */
	protected void registerTrackers() {
		// keys
		keys = new ArrayList<String>();
		keys.add(SimpleFastLAPTracker.TRACKER_KEY);
		keys.add(SimpleFastLAPTracker.TRACKER_KEY);
		keys.add(NearestNeighborTracker.TRACKER_KEY);
		// infoTexts
		infoTexts = new ArrayList<String>();
		infoTexts.add(SimpleFastLAPTracker.INFO_TEXT);
		infoTexts.add(FastLAPTracker.INFO_TEXT);
		infoTexts.add(NearestNeighborTracker.INFO_TEXT);
		// Names
		names = new ArrayList<String>();
		names.add(SimpleFastLAPTracker.NAME);
		names.add(FastLAPTracker.NAME);
		names.add(NearestNeighborTracker.NAME);
	}
	
	/**
	 * @return a new instance of the target tracker identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public SpotTracker getTracker() {
		
		final Map<String, Object> settings = model.getSettings().trackerSettings;
		final SpotCollection spots = model.getFilteredSpots();
		final Logger logger = model.getLogger();
		
		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY)) {
			return new SimpleFastLAPTracker(spots, settings, logger);
			
		} else if (currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			return new FastLAPTracker(spots, settings, logger);
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			final double maxDist = (Double) settings.get(KEY_LINKING_MAX_DISTANCE);
			return new NearestNeighborTracker(spots, maxDist, logger);
			
		} else {
			return null;
		}
	}

	/**
	 * @return the html String containing a descriptive information about the target tracker,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public String getInfoText() {
		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY)) {
			return SimpleFastLAPTracker.INFO_TEXT;
			
		} else if (currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			return FastLAPTracker.INFO_TEXT;
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			return NearestNeighborTracker.INFO_TEXT;
			
		} else {
			return null;
		}
	}
	
	/**
	 * @return the name of the target tracker,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public String getName() {
		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY)) {
			return SimpleFastLAPTracker.NAME;
			
		} else if (currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			return FastLAPTracker.NAME;
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			return NearestNeighborTracker.NAME;
			
		} else {
			return null;
		}
	}

	/**
	 * @return a new GUI panel able to configure the settings suitable for the target tracker 
	 * identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */

	public ConfigurationPanel getTrackerConfigurationPanel() 	{
		
		String trackerName = getName();
		String spaceUnits = model.getSettings().spaceUnits;
		List<String> features = model.getFeatureModel().getSpotFeatures();
		Map<String, String> featureNames = model.getFeatureModel().getSpotFeatureNames();
		
		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY)) {
			return new SimpleLAPTrackerSettingsPanel(trackerName, SimpleFastLAPTracker.INFO_TEXT, spaceUnits);
			
		} else if (currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			return new LAPTrackerSettingsPanel(trackerName, spaceUnits, features, featureNames);
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			return new NearestNeighborTrackerSettingsPanel(trackerName, NearestNeighborTracker.INFO_TEXT, spaceUnits);
			
		} else {
			return null;
		}
	}
	
	/**
	 * @return a new default settings map suitable for the target tracker identified by 
	 * the {@link #currentKey}. Settings are instantiated with default values.  
	 * If the key is unknown to this provider, <code>null</code> is returned. 
	 */
	public Map<String, Object> getDefaultSettings() {
		Map<String, Object> settings;

		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY) || currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			settings = LAPUtils.getDefaultLAPSettingsMap();
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			settings = new HashMap<String, Object>();
			settings.put(KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE);
			
		} else {
			return null;
		}
		return settings;

	}
	
	/**
	 * Marshall a settings map to a JDom element, ready for saving to XML. 
	 * The element is <b>updated</b> with new attributes.
	 * <p>
	 * Only parameters specific to the target tracker factory are marshalled.
	 * The element also always receive an attribute named {@value TrackerKeys#XML_ATTRIBUTE_DETECTOR_NAME}
	 * that saves the target {@link SpotTracker} key.
	 * 
	 * @return true if marshalling was successful. If not, check {@link #getErrorMessage()}
	 */
	public boolean marshall(final Map<String, Object> settings, Element element) {
		
		element.setAttribute(XML_ATTRIBUTE_TRACKER_NAME, currentKey);
		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY) || currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			
			boolean ok = true;
			
			// Linking
			Element linkingElement = new Element(XML_ELEMENT_NAME_LINKING);
			ok = ok & writeAttribute(settings, linkingElement, KEY_LINKING_MAX_DISTANCE, Double.class);
			// feature penalties
			@SuppressWarnings("unchecked")
			Map<String, Double> lfpm = (Map<String, Double>) settings.get(KEY_LINKING_FEATURE_PENALTIES);
			Element lfpElement = new Element(XML_ELEMENT_NAME_FEATURE_PENALTIES);
			marshallMap(lfpm, lfpElement);
			linkingElement.addContent(lfpElement);
			element.addContent(linkingElement);
			
			// Gap closing
			Element gapClosingElement = new Element(XML_ELEMENT_NAME_GAP_CLOSING);
			ok = ok & writeAttribute(settings, gapClosingElement, KEY_ALLOW_GAP_CLOSING, Boolean.class);
			ok = ok & writeAttribute(settings, gapClosingElement, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class);
			ok = ok & writeAttribute(settings, gapClosingElement, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class);
			// feature penalties
			@SuppressWarnings("unchecked")
			Map<String, Double> gcfpm = (Map<String, Double>) settings.get(KEY_GAP_CLOSING_FEATURE_PENALTIES);
			Element gcfpElement = new Element(XML_ELEMENT_NAME_FEATURE_PENALTIES);
			marshallMap(gcfpm, gcfpElement);
			gapClosingElement.addContent(gcfpElement);
			element.addContent(gapClosingElement);
			
			// Track splitting
			Element trackSplittingElement = new Element(XML_ELEMENT_NAME_SPLITTING);
			ok = ok & writeAttribute(settings, trackSplittingElement, KEY_ALLOW_TRACK_SPLITTING, Boolean.class);
			ok = ok & writeAttribute(settings, trackSplittingElement, KEY_SPLITTING_MAX_DISTANCE, Double.class);
			// feature penalties
			@SuppressWarnings("unchecked")
			Map<String, Double> tsfpm = (Map<String, Double>) settings.get(KEY_SPLITTING_FEATURE_PENALTIES);
			Element tsfpElement = new Element(XML_ELEMENT_NAME_FEATURE_PENALTIES);
			marshallMap(tsfpm, tsfpElement);
			trackSplittingElement.addContent(tsfpElement);
			element.addContent(trackSplittingElement);
			
			// Track merging
			Element trackMergingElement = new Element(XML_ELEMENT_NAME_MERGING);
			ok = ok & writeAttribute(settings, trackMergingElement, KEY_ALLOW_TRACK_MERGING, Boolean.class);
			ok = ok & writeAttribute(settings, trackMergingElement, KEY_MERGING_MAX_DISTANCE, Double.class);
			// feature penalties
			@SuppressWarnings("unchecked")
			Map<String, Double> tmfpm = (Map<String, Double>) settings.get(KEY_MERGING_FEATURE_PENALTIES);
			Element tmfpElement = new Element(XML_ELEMENT_NAME_FEATURE_PENALTIES);
			marshallMap(tmfpm, tmfpElement);
			trackMergingElement.addContent(tmfpElement);
			element.addContent(trackMergingElement);
			
			// Others
			ok = ok & writeAttribute(settings, element, KEY_CUTOFF_PERCENTILE, Double.class);
			ok = ok & writeAttribute(settings, element, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class);
			ok = ok & writeAttribute(settings, element, KEY_BLOCKING_VALUE, Double.class);
			
			return ok;
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			return writeAttribute(settings, element, KEY_LINKING_MAX_DISTANCE, Double.class);
			
		} else {

			errorMessage = "Unknow detector factory key: "+currentKey+".\n";
			return false;
		}
	}

	/**
	 * Un-marshall a JDom element to update a settings map, and sets the target 
	 * tracker of this provider from the element. 
	 * <p>
	 * Concretely: the tracker key is read from the element, and is used to set 
	 * the target {@link #currentKey} of this provider. The the specific settings 
	 * map for the targeted tracker is updated from the element.
	 * 
	 * @param element the JDom element to read from.
	 * @param settings the map to update. Is cleared prior to updating, so that it contains
	 * only the parameters specific to the target tracker.
	 * @return true if unmarshalling was successful. If not, check {@link #getErrorMessage()}
	 */
	public boolean unmarshall(Element element, final Map<String, Object> settings) {
		
		settings.clear();

		String trackerKey = element.getAttributeValue(XML_ATTRIBUTE_TRACKER_NAME);
		// Try to set the state of this provider from the key read in xml.
		boolean ok = select(trackerKey);
		if (!ok) {
			errorMessage = "Tracker key found in XML ("+trackerKey+") is unknown to this provider.\n";
			return false;
		}

		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY) || currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			StringBuilder errorHolder = new StringBuilder(); 
			
			// Linking
			Element linkingElement = element.getChild(XML_ELEMENT_NAME_LINKING);
			if (null == linkingElement) {
				errorHolder.append("Could not found the "+XML_ELEMENT_NAME_LINKING+" element in XML.\n");
				ok = false;
				
			} else {
				
				ok = ok & readDoubleAttribute(linkingElement, settings, KEY_LINKING_MAX_DISTANCE);
				// feature penalties
				Map<String, Double> lfpMap = new HashMap<String, Double>();
				Element lfpElement = linkingElement.getChild(XML_ELEMENT_NAME_FEATURE_PENALTIES);
				if (null != lfpElement) {
					ok = ok & unmarshallMap(lfpElement , lfpMap);
				}
				settings.put(KEY_LINKING_FEATURE_PENALTIES, lfpMap);
			}
			
			// Gap closing
			Element gapClosingElement = element.getChild(XML_ELEMENT_NAME_GAP_CLOSING);
			if (null == gapClosingElement) {
				errorHolder.append("Could not found the "+XML_ELEMENT_NAME_GAP_CLOSING+" element in XML.\n");
				ok = false;
				
			} else {

				ok = ok & readBooleanAttribute(gapClosingElement, settings, KEY_ALLOW_GAP_CLOSING);
				ok = ok & readIntegerAttribute(gapClosingElement, settings, KEY_GAP_CLOSING_MAX_FRAME_GAP);
				ok = ok & readDoubleAttribute(gapClosingElement, settings, KEY_GAP_CLOSING_MAX_DISTANCE);
				// feature penalties
				Map<String, Double> gcfpm = new HashMap<String, Double>();
				Element gcfpElement = gapClosingElement.getChild(XML_ELEMENT_NAME_FEATURE_PENALTIES);
				if (null != gcfpElement) {
					ok = ok & unmarshallMap(gcfpElement, gcfpm);
				}
				settings.put(KEY_GAP_CLOSING_FEATURE_PENALTIES, gcfpm);
			}

			// Track splitting
			Element trackSplittingElement = element.getChild(XML_ELEMENT_NAME_SPLITTING);
			if (null == trackSplittingElement) {
				errorHolder.append("Could not found the "+XML_ELEMENT_NAME_SPLITTING+" element in XML.\n");
				ok = false;
				
			} else {
				
				ok = ok & readBooleanAttribute(trackSplittingElement, settings, KEY_ALLOW_TRACK_SPLITTING);
				ok = ok & readDoubleAttribute(trackSplittingElement, settings, KEY_SPLITTING_MAX_DISTANCE);
				// feature penalties
				Map<String, Double> tsfpm = new HashMap<String, Double>();
				Element tsfpElement = trackSplittingElement.getChild(XML_ELEMENT_NAME_FEATURE_PENALTIES);
				if (null != tsfpElement) {
					ok = ok & unmarshallMap(tsfpElement, tsfpm);
				}
				settings.put(KEY_SPLITTING_FEATURE_PENALTIES, tsfpm);
			}

			// Track merging
			Element trackMergingElement = element.getChild(XML_ELEMENT_NAME_MERGING);
			if (null == trackMergingElement) {
				errorHolder.append("Could not found the "+XML_ELEMENT_NAME_MERGING+" element in XML.\n");
				ok = false;

			} else {

				ok = ok & readBooleanAttribute(trackMergingElement, settings, KEY_ALLOW_TRACK_MERGING);
				ok = ok & readDoubleAttribute(trackMergingElement, settings, KEY_MERGING_MAX_DISTANCE);
				// feature penalties
				Map<String, Double> tmfpm = new HashMap<String, Double>();
				Element tmfpElement = trackMergingElement.getChild(XML_ELEMENT_NAME_FEATURE_PENALTIES);
				if (null != tmfpElement) {
					ok = ok & unmarshallMap(tmfpElement, tmfpm);
				}
				settings.put(KEY_MERGING_FEATURE_PENALTIES, tmfpm);
			}

			// Others
			ok = ok & readDoubleAttribute(element, settings, KEY_CUTOFF_PERCENTILE);
			ok = ok & readDoubleAttribute(element, settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR);
			ok = ok & readDoubleAttribute(element, settings, KEY_BLOCKING_VALUE);

			if (!ok) {
				errorMessage = errorHolder.toString();
			}
			return ok;

		} else {

			errorMessage = "Unknow tracker key: "+currentKey+".\n";
			return false;

		}
	}


	/**  @return a list of the tracker keys available through this provider.  */
	public List<String> getTrackerKeys() {
		return keys;
	}

	/**  @return a list of the tracker info texts available through this provider.  */
	public List<String> getTrackerInfoTexts() {
		return infoTexts;
	}
	
	/**  @return a list of the tracker names available through this provider.  */
	public List<String> getTrackerNames() {
		return names;
	}

	/**
	 * Check the validity of the given settings map for the target {@link SpotDetector}
	 * set in this provider. The validity check is strict: we check that all needed parameters
	 * are here and are of the right class, and that there is no extra unwanted parameters.
	 * @return  true if the settings map can be used with the target factory. If not, check {@link #getErrorMessage()}
	 */
	public boolean checkSettingsValidity(final Map<String, Object> settings) {
		if (null == settings) {
			errorMessage = "Settings map is null.\n";
			return false;
		}
		
		StringBuilder str = new StringBuilder();

		if (currentKey.equals(FastLAPTracker.TRACKER_KEY) 
				|| currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY)) {

			// Check non-map parameters
			boolean ok = true;
			// Linking
			ok = ok & checkParameter(settings, KEY_LINKING_MAX_DISTANCE, Double.class, str);
			ok = ok & checkFeatureMap(settings, KEY_LINKING_FEATURE_PENALTIES, str);
			// Gap-closing
			ok = ok & checkParameter(settings, KEY_ALLOW_GAP_CLOSING, Boolean.class, str);
			ok = ok & checkParameter(settings, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class, str);
			ok = ok & checkParameter(settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str);
			ok = ok & checkFeatureMap(settings, KEY_GAP_CLOSING_FEATURE_PENALTIES, str);
			// Splitting
			ok = ok & checkParameter(settings, KEY_ALLOW_TRACK_SPLITTING, Boolean.class, str);
			ok = ok & checkParameter(settings, KEY_SPLITTING_MAX_DISTANCE, Double.class, str);
			ok = ok & checkFeatureMap(settings, KEY_SPLITTING_FEATURE_PENALTIES, str);
			// Merging
			ok = ok & checkParameter(settings, KEY_ALLOW_TRACK_MERGING, Boolean.class, str);
			ok = ok & checkParameter(settings, KEY_MERGING_MAX_DISTANCE, Double.class, str);
			ok = ok & checkFeatureMap(settings, KEY_MERGING_FEATURE_PENALTIES, str);
			// Others
			ok = ok & checkParameter(settings, KEY_CUTOFF_PERCENTILE, Double.class, str);
			ok = ok & checkParameter(settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str);
			ok = ok & checkParameter(settings, KEY_BLOCKING_VALUE, Double.class, str);
			
			// Check keys 
			List<String> mandatoryKeys = new ArrayList<String>();
			mandatoryKeys.add(KEY_LINKING_MAX_DISTANCE);
			mandatoryKeys.add(KEY_ALLOW_GAP_CLOSING);
			mandatoryKeys.add(KEY_GAP_CLOSING_MAX_DISTANCE);
			mandatoryKeys.add(KEY_GAP_CLOSING_MAX_FRAME_GAP);
			mandatoryKeys.add(KEY_ALLOW_TRACK_SPLITTING);
			mandatoryKeys.add(KEY_SPLITTING_MAX_DISTANCE);
			mandatoryKeys.add(KEY_ALLOW_TRACK_MERGING);
			mandatoryKeys.add(KEY_MERGING_MAX_DISTANCE);
			List<String> optionalKeys = new ArrayList<String>();
			optionalKeys.add(KEY_LINKING_FEATURE_PENALTIES);
			optionalKeys.add(KEY_GAP_CLOSING_FEATURE_PENALTIES);
			optionalKeys.add(KEY_SPLITTING_FEATURE_PENALTIES);
			optionalKeys.add(KEY_MERGING_FEATURE_PENALTIES);
			ok = ok & checkMapKeys(settings, mandatoryKeys, optionalKeys, str);
			
			if (!ok) {
				errorMessage = str.toString();
			}
			return ok;

		} else if (currentKey.equals(DownsampleLogDetectorFactory.DETECTOR_KEY)) {

			boolean ok = true;
			ok = ok & checkParameter(settings, KEY_LINKING_MAX_DISTANCE, Double.class, str);
			List<String> mandatoryKeys = new ArrayList<String>();
			mandatoryKeys.add(KEY_LINKING_MAX_DISTANCE);
			ok = ok & checkMapKeys(settings, mandatoryKeys, null, str);

			if (!ok) {
				errorMessage = str.toString();
			}
			return ok;

		} else {

			errorMessage = "Unknow detector factory key: "+currentKey+".\n";
			return false;

		}
	}
	

	private static String echoFeaturePenalties(final Map<String, Double> featurePenalties) {
		String str = "";
		if (featurePenalties.isEmpty()) 
			str += "    - no feature penalties\n";
		else {
			str += "    - with feature penalties:\n";
			for (String feature : featurePenalties.keySet()) {
				str += "      - "+feature.toString() + ": weight = " + String.format("%.1f", featurePenalties.get(feature)) + '\n';
			}
		}
		return str;

	}


}