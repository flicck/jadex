package jadex.bridge.service.types.security;

import java.util.Map;
import java.util.Set;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.component.IMsgHeader;
import jadex.bridge.service.annotation.GuiClassName;
import jadex.bridge.service.annotation.GuiClassNames;
import jadex.bridge.service.annotation.Reference;
import jadex.bridge.service.annotation.SecureTransmission;
import jadex.commons.Tuple2;
import jadex.commons.future.IFuture;

/**
 *  The security service is responsible for
 *  validating (remote) requests.
 *  Currently only platform level authentication
 *  is provided. More fine grained control on
 *  service/method level based on user/group
 *  access rights is planned for the mid-term future.
 */
//Safe to be allowed remotely, as it can only be called, when platform access is granted.
//Putting method in service allows security settings to be administered using remote JCCs.
@GuiClassNames({
	@GuiClassName("jadex.tools.security.SecuritySettingsPanel"),
	@GuiClassName("jadex.android.controlcenter.settings.SecuritySettings")
})
public interface ISecurityService
{
	/** Allow the use of the local platform secret. */
	public static final String PROPERTY_USESECRET = "usesecret";
	
	/** Print the local platform secret on start. */
	public static final String PROPERTY_PRINTSECRET = "printsecret";
	
	/** The local platform secret. */
	public static final String PROPERTY_PLATFORMSECRET = "platformsecret";
	
	/** Remote platform(s) to include. */
	public static final String PROPERTY_REMOTEPLATFORM = "remoteplatform";

	/** The Remote platform secret(s). */
	public static final String PROPERTY_REMOTEPLATFORMSECRET = "remoteplatformsecret";
	
	/** Network(s) to include. */
	public static final String PROPERTY_NETWORK = "network";

	/** The network secret(s). */
	public static final String PROPERTY_NETWORKSECRET = "networksecret";
	
	//-------- message-level encryption/authentication -------
	
	/**
	 *  Encrypts and signs the message for a receiver.
	 *  
	 *  @param receiver The receiver.
	 *  @param content The content
	 *  @return Encrypted/signed message.
	 */
	public @Reference IFuture<byte[]> encryptAndSign(IMsgHeader header, byte[] content);
//	public @Reference IFuture<byte[]> encryptAndSign(@Reference Map<String, Object> header, @Reference byte[] content);
	
	/**
	 *  Decrypt and authenticates the message from a sender.
	 *  
	 *  @param sender The sender.
	 *  @param content The content.
	 *  @return Decrypted/authenticated message or null on invalid message.
	 */
	public IFuture<Tuple2<IMsgSecurityInfos,byte[]>> decryptAndAuth(IComponentIdentifier sender, byte[] content);
	
	/**
	 *  Gets the secret of a platform if available.
	 * 
	 *  @param cid ID of the platform.
	 *  @return Encoded secret or null.
	 */
	public IFuture<String> getEncodedPlatformSecret(IComponentIdentifier cid);
	
	/**
	 *  Sets the secret of a platform.
	 * 
	 *  @param cid ID of the platform.
	 *  @return Encoded secret or null.
	 */
	public IFuture<Void> setEncodedPlatformSecret(IComponentIdentifier cid, String secret);
}
