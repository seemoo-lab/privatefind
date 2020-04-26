package de.seemoo.blefinderapp.db;

import android.location.Location;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.ToOne;

import java.util.Date;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import de.seemoo.blefinderapp.db.gen.DaoSession;
import de.seemoo.blefinderapp.db.gen.KnownDeviceDao;
import org.greenrobot.greendao.annotation.NotNull;
import org.jetbrains.annotations.Nullable;

import de.seemoo.blefinderapp.db.gen.HistoryItemDao;

@Entity
public class HistoryItem {
	@Id
	private Long id;
	private long deviceId;
	@ToOne(joinProperty = "deviceId")
	private KnownDevice device;
	private Date timestamp;
	private String eventType;
	private double latitude;
	private double longitude;
	private Date locationTimestamp;
	private float locationAccuracy;

	@Keep
	public HistoryItem(long deviceId, String eventType, Location location) {
		this.deviceId = deviceId;
		this.eventType = eventType;
		this.timestamp = new Date();
		setFromLocation(location);
	}
	public void setFromLocation(@Nullable Location location) {
		setLatitude(location.getLatitude());
		setLongitude(location.getLongitude());
		setLocationAccuracy(location.getAccuracy());
		setLocationTimestamp(new Date(location.getTime()));
	}

	/** Used to resolve relations */
	@Generated(hash = 2040040024)
	private transient DaoSession daoSession;
	/** Used for active entity operations. */
	@Generated(hash = 2050166950)
	private transient HistoryItemDao myDao;
	@Generated(hash = 270556220)
	public HistoryItem(Long id, long deviceId, Date timestamp, String eventType,
			double latitude, double longitude, Date locationTimestamp,
			float locationAccuracy) {
		this.id = id;
		this.deviceId = deviceId;
		this.timestamp = timestamp;
		this.eventType = eventType;
		this.latitude = latitude;
		this.longitude = longitude;
		this.locationTimestamp = locationTimestamp;
		this.locationAccuracy = locationAccuracy;
	}
	@Keep
	public HistoryItem() {
		this.timestamp = new Date();
	}
	public Long getId() {
		return this.id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public long getDeviceId() {
		return this.deviceId;
	}
	public void setDeviceId(long deviceId) {
		this.deviceId = deviceId;
	}
	public Date getTimestamp() {
		return this.timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public String getEventType() {
		return this.eventType;
	}
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
	public double getLatitude() {
		return this.latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return this.longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public Date getLocationTimestamp() {
		return this.locationTimestamp;
	}
	public void setLocationTimestamp(Date locationTimestamp) {
		this.locationTimestamp = locationTimestamp;
	}
	public float getLocationAccuracy() {
		return this.locationAccuracy;
	}
	public void setLocationAccuracy(float locationAccuracy) {
		this.locationAccuracy = locationAccuracy;
	}
	@Generated(hash = 708752895)
	private transient Long device__resolvedKey;
	/** To-one relationship, resolved on first access. */
	@Generated(hash = 1892972050)
	public KnownDevice getDevice() {
		long __key = this.deviceId;
		if (device__resolvedKey == null || !device__resolvedKey.equals(__key)) {
			final DaoSession daoSession = this.daoSession;
			if (daoSession == null) {
				throw new DaoException("Entity is detached from DAO context");
			}
			KnownDeviceDao targetDao = daoSession.getKnownDeviceDao();
			KnownDevice deviceNew = targetDao.load(__key);
			synchronized (this) {
				device = deviceNew;
				device__resolvedKey = __key;
			}
		}
		return device;
	}
	/** called by internal mechanisms, do not call yourself. */
	@Generated(hash = 859424328)
	public void setDevice(@NotNull KnownDevice device) {
		if (device == null) {
			throw new DaoException(
					"To-one property 'deviceId' has not-null constraint; cannot set to-one to null");
		}
		synchronized (this) {
			this.device = device;
			deviceId = device.getId();
			device__resolvedKey = deviceId;
		}
	}
	/**
	 * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
	 * Entity must attached to an entity context.
	 */
	@Generated(hash = 128553479)
	public void delete() {
		if (myDao == null) {
			throw new DaoException("Entity is detached from DAO context");
		}
		myDao.delete(this);
	}
	/**
	 * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
	 * Entity must attached to an entity context.
	 */
	@Generated(hash = 1942392019)
	public void refresh() {
		if (myDao == null) {
			throw new DaoException("Entity is detached from DAO context");
		}
		myDao.refresh(this);
	}
	/**
	 * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
	 * Entity must attached to an entity context.
	 */
	@Generated(hash = 713229351)
	public void update() {
		if (myDao == null) {
			throw new DaoException("Entity is detached from DAO context");
		}
		myDao.update(this);
	}
	/** called by internal mechanisms, do not call yourself. */
	@Generated(hash = 668339374)
	public void __setDaoSession(DaoSession daoSession) {
		this.daoSession = daoSession;
		myDao = daoSession != null ? daoSession.getHistoryItemDao() : null;
	}


}
