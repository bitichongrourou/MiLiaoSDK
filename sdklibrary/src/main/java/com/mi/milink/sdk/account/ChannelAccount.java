package com.mi.milink.sdk.account;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.account.manager.RSAPublicKey;
import com.mi.milink.sdk.account.manager.RSAPublicKey.PublicKeyAndId;
import com.mi.milink.sdk.config.ConfigManager;
import com.mi.milink.sdk.debug.MiLinkLog;

import android.text.TextUtils;

public class ChannelAccount extends AnonymousAccount {
	
	private static final String PREF_CHANNLE_PUB_KEY_SUB = ":";
	
	int keepAliveTime = 5 * 60 * 1000 ;//默认连接保持10分钟
	
	public ChannelAccount() {
        super();
        initPubKey();
    }
	
	public  void initPubKey() {

		Set<String> set = ConfigManager.getInstance().getChannelPubKeys();
		if (set == null) {
			set = new HashSet<String>();
		}

		if (set.size() > 0) {
			return;
		}

		Set<PublicKeyAndId> setPubkList =	new  RSAPublicKey().getPublicKeySet();
		for (PublicKeyAndId p : setPubkList) {

			set.add(buildStoreValue(p));
		}
		ConfigManager.getInstance().updateChannelPubKeySet(set);
	}
	
	public  String buildStoreValue(PublicKeyAndId pubKeyAndId) {
		String valchannelStoreKey = buildChannelPubKey(pubKeyAndId.id + "", pubKeyAndId.key);
		return valchannelStoreKey;
	}
	
	@Override
	protected String getPrefFileName() {
		return "milink_channel_account";
	}

	@Override
    protected String getTag() {
		return String.format("ChannelAccount[No:%d]",mNo);
	}

	@Override
	protected int getAccountType() {
		return MiAccountManager.ACCOUNT_TYPE_CHANNEL;
	}
		
	private String buildChannelPubKey(String keyId, String pubKey) {

		String keyIdStr = keyId + "";
		return keyIdStr + PREF_CHANNLE_PUB_KEY_SUB + pubKey;
	}
	
	
	private PublicKeyAndId getRandomPublicKeyAndId() {
		Set<String> channelPubKey = ConfigManager.getInstance().getChannelPubKeys();

		if (channelPubKey == null) {
			MiLinkLog.v(getTag(), "getRandomPublicKeyAndId is null");
			return null;
		}

		Map<String, String> map = new HashMap<String, String>();
		for (String pubkey : channelPubKey) {
			String[] arr = pubkey.split(PREF_CHANNLE_PUB_KEY_SUB);
			if (arr.length == 2) {
				map = new HashMap<String, String>();
				String id = arr[0];
				String key = arr[1];
				map.put(id, key);
			}
		}

		if (map == null || map.size() == 0) {
			MiLinkLog.v(getTag(), "getRandomPublicKeyAndId parseChannelPubKey map is null || size=0");
			return null;
		}
		
		String[] keys = map.keySet().toArray(new String[0]);
		Random random = new Random();
		String randomKey = keys[random.nextInt(map.size())];
		String randomValue = map.get(randomKey);
		PublicKeyAndId p = new PublicKeyAndId(randomKey, randomValue);
		MiLinkLog.d(getTag(), "getRandomPublicKeyAndId find PublicKeyAndId id=" + randomKey + ", key=" + randomValue);
		return p;
	}
	
	
	private void generateServiceToken() {
		PublicKeyAndId keyAndId = getRandomPublicKeyAndId();
		if (keyAndId == null) {
			keyAndId = RSAPublicKey.getPublicKeyAndId();
		}

		this.mServiceToken = keyAndId.id;
		this.mSSecurity = keyAndId.key;
		MiLinkLog.v(getTag(),
				"generateServiceTokenAndSSecurity mServiceToken=" + mServiceToken + ",mSSecurity=" + mSSecurity);
	} 
	
	@Override
	public void generateServiceTokenAndSSecurity() {
		if ("0".equals(mServiceToken) || TextUtils.isEmpty(mSSecurity)) {
			// 先从存储中查到信息.如果没有则走默认
			generateServiceToken();
		}
	}

	@Override
    public void setChannelPubKey(Map<Integer, String> channelPubKeyMap) {

		Set<String> channelStoreKey = new HashSet<String>();

		for (Integer keyId : channelPubKeyMap.keySet()) {

			String pubKey = channelPubKeyMap.get(keyId);
			String valchannelStoreKey = buildChannelPubKey(keyId+"", pubKey);
			channelStoreKey.add(valchannelStoreKey);
			MiLinkLog.v(getTag(),
					"setChannelPubKey keyId:"+keyId+" ,pubKey:"+pubKey.toString() );
		}
		ConfigManager.getInstance().updateChannelPubKeySet(channelStoreKey);
		generateServiceToken();
	}

	@Override
    public void DelChannelPubKey() {
		String valchannelStoreKey = buildChannelPubKey(this.mServiceToken + "", this.mSSecurity);
		ConfigManager.getInstance().deleteChannelPubKey(valchannelStoreKey);
		generateServiceToken();
	}
	
	
	@Override
	public void setKeepAliveTime(int keepAliveTime) {
		this.keepAliveTime = keepAliveTime * 1000;
	}
	@Override
	public int getKeepAliveTime() {
		MiLinkLog.v(getTag(),
				"get keepAliveTime:" + this.keepAliveTime);
		return this.keepAliveTime;
	}
}
