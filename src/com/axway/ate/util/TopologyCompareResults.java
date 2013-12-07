package com.axway.ate.util;

import java.util.ArrayList;
import java.util.List;

import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology.EntityType;

public class TopologyCompareResults {
	private static final String TAG = TopologyCompareResults.class.getSimpleName();

	public class Entry {
		public EntityType itemType;
		public String id;
		public String name;
		public Object data;
		
		public Entry(EntityType itemType, String id, String name) {
			super();
			this.itemType = itemType;
			this.id = id;
			this.name = name;
			data = null;
		}
		
		public Entry(EntityType itemType, String id, String name, Object data) {
			this(itemType, id, name);
			this.data = data;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Entry [itemType=");
			builder.append(itemType);
			builder.append(", id=");
			builder.append(id);
			builder.append(", name=");
			builder.append(name);
			builder.append(", data=");
			builder.append(data);
			builder.append("]");
			return builder.toString();
		}
	}
	
	private List<Entry> toBeAdded;
	private List<Entry> toBeChanged;
	private List<Entry> toBeRemoved;
	
	public TopologyCompareResults() {
		super();
		toBeAdded = null;
		toBeChanged = null;
		toBeRemoved = null;
	}

	public Entry createEntry(Host h) {
		if (h == null)
			return null;
		return new Entry(EntityType.Host, h.getId(), h.getName());
	}

	public Entry createEntry(Group g) {
		if (g == null)
			return null;
		return new Entry(EntityType.Group, g.getId(), g.getName());
	}

	public Entry createEntry(Service s) {
		if (s == null)
			return null;
		return new Entry(EntityType.Gateway, s.getId(), s.getName());
	}
	
	public void addAddedEntry(Entry e) {
		if (e == null)
			return;
		if (toBeAdded == null)
			toBeAdded = new ArrayList<Entry>();
		toBeAdded.add(e);
	}
	
	public void removeAddedEntry(Entry e) {
		if (e == null || toBeAdded == null)
			return;
		toBeAdded.remove(e);
	}
	
	public void addChangedEntry(Entry e) {
		if (e == null)
			return;
		if (toBeChanged == null)
			toBeChanged = new ArrayList<Entry>();
		toBeChanged.add(e);
	}
	
	public void removeChangedEntry(Entry e) {
		if (e == null || toBeChanged == null)
			return;
		toBeChanged.remove(e);
	}
	
	public void addRemovedEntry(Entry e) {
		if (e == null)
			return;
		if (toBeRemoved == null)
			toBeRemoved = new ArrayList<Entry>();
		toBeRemoved.add(e);
	}
	
	public void removeRemovedEntry(Entry e) {
		if (e == null || toBeRemoved == null)
			return;
		toBeRemoved.remove(e);
	}

	public List<Entry> getToBeAdded() {
		if (toBeAdded == null)
			toBeAdded = new ArrayList<Entry>();
		return toBeAdded;
	}

	public List<Entry> getToBeChanged() {
		if (toBeChanged == null)
			toBeChanged = new ArrayList<Entry>();
		return toBeChanged;
	}

	public List<Entry> getToBeRemoved() {
		if (toBeRemoved == null)
			toBeRemoved = new ArrayList<Entry>();
		return toBeRemoved;
	}
	
	public String prettyPrint() {
		StringBuilder sb = new StringBuilder();
		boolean inSync = true;
		sb.append("\nTo Be Added: ");
		if (getToBeAdded().size() == 0)
			sb.append("none");
		else {
			inSync = false;
			for (TopologyCompareResults.Entry e: getToBeAdded()) {
				sb.append("\n  ").append(e.itemType).append(" id=").append(e.id).append(", ").append(e.name);
			}
		}
		sb.append("\n\nTo Be Changed: ");
		if (getToBeChanged().size() == 0)
			sb.append("none");
		else {
			inSync = false;
			for (TopologyCompareResults.Entry e: getToBeChanged()) {
				sb.append("\n  ").append(e.itemType).append(" id=").append(e.id).append(", ").append(e.name);
			}
		}
		sb.append("\n\nTo Be Removed: ");
		if (getToBeRemoved().size() == 0)
			sb.append("none");
		else {
			inSync = false;
			for (TopologyCompareResults.Entry e: getToBeRemoved()) {
				sb.append("\n  ").append(e.itemType).append(" id=").append(e.id).append(", ").append(e.name);
			}
		}
		if (inSync)
			sb.append("\n\nThe client and server topologies are in sync!");
		else
			sb.append("\n\nThe changes listed above would have to be posted to the server in order to sync the topologies.");
		sb.append("\n");
		return sb.toString();
	}
}
