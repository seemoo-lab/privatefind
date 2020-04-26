package de.seemoo.blefinderapp.db;

import android.bluetooth.BluetoothDevice;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.Unique;

import java.security.SecureRandom;
import java.util.Date;

import de.seemoo.blefinderapp.Helper;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class KnownDevice {
	@Id
	private Long id;
	@Unique
	private  String bluetoothAddress;
	private  String bluetoothName;
	private  String displayName;
	private  byte[] e2eKey;
	private  String cloudAccessToken;
	private  int  e2eCounter;
	private  byte localLinkLossAlertLevel;
	private  byte  remoteLinkLossAlertLevel;
	private Date registrationDate;
	private  Date lastSeenDate;
	private  Date lastConnectedDate;
	private  Date lastReportDate;


	@Generated(hash = 677925859)
	public KnownDevice(Long id, String bluetoothAddress, String bluetoothName,
			String displayName, byte[] e2eKey, String cloudAccessToken, int e2eCounter,
			byte localLinkLossAlertLevel, byte remoteLinkLossAlertLevel,
			Date registrationDate, Date lastSeenDate, Date lastConnectedDate,
			Date lastReportDate) {
		this.id = id;
		this.bluetoothAddress = bluetoothAddress;
		this.bluetoothName = bluetoothName;
		this.displayName = displayName;
		this.e2eKey = e2eKey;
		this.cloudAccessToken = cloudAccessToken;
		this.e2eCounter = e2eCounter;
		this.localLinkLossAlertLevel = localLinkLossAlertLevel;
		this.remoteLinkLossAlertLevel = remoteLinkLossAlertLevel;
		this.registrationDate = registrationDate;
		this.lastSeenDate = lastSeenDate;
		this.lastConnectedDate = lastConnectedDate;
		this.lastReportDate = lastReportDate;
	}

	@Generated(hash = 460812493)
	public KnownDevice() {
	}

	@Keep
	public KnownDevice(BluetoothDevice dev) {
		bluetoothAddress = dev.getAddress();
		bluetoothName = dev.getName();
		if (Helper.nullOrEmpty(bluetoothName)) bluetoothName = bluetoothAddress;
		displayName = dev.getName();
		if (Helper.nullOrEmpty(displayName)) displayName = bluetoothName;
		generateRandomKeys();
	}

	public void generateRandomKeys() {
		SecureRandom random = new SecureRandom();
		e2eKey = new byte[16]; // 128 bits are converted to 16 bytes;
		random.nextBytes(e2eKey);
		e2eCounter = 1;
	}

	@Override
	public String toString() {
		return "Bluetooth address: " + bluetoothAddress + "\n" +
				"Bluetooth name: " + bluetoothName + "\n" +
				"Access Token: " + cloudAccessToken + "\n" +
				"End to End Encryption Key: " + Helper.bytesToHex(e2eKey) + "\n" +
				"End to End Counter: " + e2eCounter + "\n" +
				"Registration Date: " + registrationDate + "\n" +
				"Last seen: " + lastSeenDate + "\n" +
				"Last reported: " + lastReportDate + "\n";

	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBluetoothAddress() {
		return this.bluetoothAddress;
	}

	public void setBluetoothAddress(String bluetoothAddress) {
		this.bluetoothAddress = bluetoothAddress;
	}

	public String getBluetoothName() {
		return this.bluetoothName;
	}

	public void setBluetoothName(String bluetoothName) {
		this.bluetoothName = bluetoothName;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public byte[] getE2eKey() {
		return this.e2eKey;
	}

	public void setE2eKey(byte[] e2eKey) {
		this.e2eKey = e2eKey;
	}

	public String getCloudAccessToken() {
		return this.cloudAccessToken;
	}

	public void setCloudAccessToken(String cloudAccessToken) {
		this.cloudAccessToken = cloudAccessToken;
	}

	public int getE2eCounter() {
		return this.e2eCounter;
	}

	public void setE2eCounter(int e2eCounter) {
		this.e2eCounter = e2eCounter;
	}

	public byte getLocalLinkLossAlertLevel() {
		return this.localLinkLossAlertLevel;
	}

	public void setLocalLinkLossAlertLevel(byte localLinkLossAlertLevel) {
		this.localLinkLossAlertLevel = localLinkLossAlertLevel;
	}

	public byte getRemoteLinkLossAlertLevel() {
		return this.remoteLinkLossAlertLevel;
	}

	public void setRemoteLinkLossAlertLevel(byte remoteLinkLossAlertLevel) {
		this.remoteLinkLossAlertLevel = remoteLinkLossAlertLevel;
	}

	public Date getRegistrationDate() {
		return this.registrationDate;
	}

	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}

	public Date getLastSeenDate() {
		return this.lastSeenDate;
	}

	public void setLastSeenDate(Date lastSeenDate) {
		this.lastSeenDate = lastSeenDate;
	}

	public Date getLastConnectedDate() {
		return this.lastConnectedDate;
	}

	public void setLastConnectedDate(Date lastConnectedDate) {
		this.lastConnectedDate = lastConnectedDate;
	}

	public Date getLastReportDate() {
		return this.lastReportDate;
	}

	public void setLastReportDate(Date lastReportDate) {
		this.lastReportDate = lastReportDate;
	}
}
