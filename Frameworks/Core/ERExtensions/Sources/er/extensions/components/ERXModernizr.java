package er.extensions.components;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSNotificationCenter;

import er.extensions.appserver.ERXSession;
import er.extensions.foundation.ERXProperties;
import er.extensions.foundation.ERXValueUtilities;

/**
 * ERXModernizr uses the Modernizr library to detect what javascript
 * capabilities a client browser has and then posts those values back
 * to the server to store them on the session. To use it, just drop it
 * in your page wrapper. For storage, this component relies on the
 * {@link ERXSession#objectStore()}. At the moment the modernizr data
 * is stored on the session, a notification is fired using the session
 * as the notification object. This is to permit logging of modernizr data
 * for later analysis. It also permits you to copy the modernizer data
 * elsewhere if the session's objectStore is not appropriate for your
 * needs.
 * 
 * It is recommended that in your ERXSession subclass, you set
 * <code>_javaScriptEnabled == Boolean.FALSE;</code> in the constructor.  
 * By default, it will be set to true when {@link ERXSession#javaScriptEnabled()} 
 * is called. This component will set the value to true whenever the 
 * modernizr data updates as a result of an ajax call... thus ensuring 
 * no false positives.
 * 
 * As a convenience, this component defines two properties for naming the
 * framework and filename for the modernizer script. This is to allow the
 * min.js for deployment while the full js can be used in development. The
 * component bindings are used as a default value should you prefer to use
 * bindings instead of properties.
 * 
 * @author Ramsey Gurley
 *
 */
public class ERXModernizr extends ERXStatelessComponent {
	//TODO add support for additional tests
	
	public static final String FRAMEWORK_NAME_PROPERTY = "er.extensions.components.ERXModernizr.modernizrFrameworkName";
	public static final String FILE_NAME_PROPERTY = "er.extensions.components.ERXModernizr.modernizrFileName";
	public static final String MODERNIZR_KEY = "modernizr";
	public static final String MODERNIZR_UPDATED_NOTIFICATION = "ERXModernizrUpdatedNotification";

	public ERXModernizr(WOContext context) {
		super(context);
	}

	/**
	 * The filename for the modernizr javascript file. A default value
	 * can be provided with component bindings if no property is declared.
	 * 
	 * @return a file name
	 * @property er.extensions.components.ERXModernizr.modernizrFileName
	 */
	public String filename() {
		String filename = ERXProperties.stringForKeyWithDefault(FILE_NAME_PROPERTY, stringValueForBinding("filename"));
		return filename;
	}

	/**
	 * The name of the framework where the modernizr javascript file is
	 * found. A default value can be provided with component bindings if 
	 * no property is declared.
	 * 
	 * @return the framework name
	 * @property er.extensions.components.ERXModernizr.modernizrFrameworkName
	 */
	public String framework() {
		String framework = ERXProperties.stringForKeyWithDefault(FRAMEWORK_NAME_PROPERTY, stringValueForBinding("framework"));
		return framework;
	}

	/**
	 * The ajax request URL for this component.
	 * @return the post URL for the ajax post request
	 */
	public String postURL() {
		String key = WOApplication.application().ajaxRequestHandlerKey();
		return context().componentActionURL(key);
	}

	/**
	 * Returns true if the component should include a script to post modernizr
	 * data back to the server. This remains true until the modernizr data is
	 * captured.
	 * 
	 * @return true if ajax script should be included
	 */
	public boolean shouldPostData() {
		ERXSession session = (ERXSession)context().session();
		return !(session.objectStore().valueForKey(MODERNIZR_KEY) instanceof NSDictionary);
	}
	
	/**
	 * Overridden to capture the modernizr data being sent from the client.
	 */
	public WOActionResults invokeAction(WORequest request, WOContext context) {
		WOActionResults result = super.invokeAction(request, context);
		NSArray<String> keys = request.formValueKeys();
		NSMutableDictionary<String, Boolean> modernizr = new NSMutableDictionary<String, Boolean>();
		for(String key: keys) {
			Boolean value = ERXValueUtilities.BooleanValueWithDefault(request.stringFormValueForKey(key), Boolean.FALSE);
			modernizr.setObjectForKey(value, key);
		}
		ERXSession session = ERXSession.session();
		session.objectStore().takeValueForKey(modernizr.immutableClone(), MODERNIZR_KEY);
		session.setJavaScriptEnabled(true);
		postNotification(session);
		return result;
	}

	/**
	 * Called to post a notification whenever modernizr data is stored
	 * on the session's object store.
	 * 
	 * @param session
	 */
	protected void postNotification(ERXSession session) {
		NSNotificationCenter nc = NSNotificationCenter.defaultCenter();
		nc.postNotification(MODERNIZR_UPDATED_NOTIFICATION, session);
	}
}