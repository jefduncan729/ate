package com.axway.ate.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.util.BitmapFactory;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;
import com.vordel.api.topology.model.Topology.ServiceType;

public class TopologyAdapter extends BaseAdapter {

	private Topology t;
	private LayoutInflater inflater;
	private List<Entry> entries;
	private BitmapFactory bmpFactory;
	private String source;
	private int flags;
	
	public TopologyAdapter(Context ctx, Topology item, String source) {
		super();
		t = item;
		this.source = source;
		inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		bmpFactory = BitmapFactory.getInstance(ctx);
		createEntries();
	}
	
	public TopologyAdapter(Context ctx, Topology item, String source, int flags) {
		this(ctx, item, source);
		this.flags = flags;
	}

	public class Entry {
		public EntityType itemType;
		public String id;
		public String name;
		public int data;
		
		public Entry(EntityType itemType, String id, String name) {
			super();
			this.itemType = itemType;
			this.id = id;
			this.name = name;
		}
		
		public Entry(EntityType itemType, String id, String name, int data) {
			this(itemType, id, name);
			this.data = data;
		}
	}

	private void createEntries() {
		entries = null;
		if (t == null)
			return;
		entries = new ArrayList<Entry>();
		//The 'NodeManager' entry is a special entry which displays the source of the topology (file or url)
		entries.add(new Entry(EntityType.NodeManager, "", "Loaded from " + source, -1));
		if (t.getHosts() != null && t.getHosts().size() > 0) {
			for (Host h: t.getHosts()) {
				int n = t.getServicesOnHost(h.getId(), ServiceType.gateway).size();
				entries.add(new Entry(EntityType.Host, h.getId() == null ? "adding..." : h.getId(), h.getName(), n));
			}
		}
		if (t.getGroups() != null && t.getGroups().size() > 0) {
			boolean addGrp;
			for (Group g: t.getGroups()) {
				addGrp = true;
				Collection<Service> nms = g.getServicesByType(ServiceType.nodemanager);
				for (Service s: nms) {
					if (Topology.isAdminNodeManager(s)) {
						addGrp = false;
						break;
					}
				}
				if (addGrp) {
					Collection<Service> gws = g.getServicesByType(ServiceType.gateway);
					entries.add(new Entry(EntityType.Group, g.getId() == null ? "adding..." : g.getId(), g.getName(), gws.size()));
					if (gws.size() > 0) {
						for (Service s: gws) {
							entries.add(new Entry(EntityType.Gateway, g.getId()+"/"+(s.getId() == null ? "adding..." : s.getId()), s.getName()));
						}
					}
				}
			}
		}
	}
	
	@Override
	public int getCount() {
		if (entries == null)
			return 0;
		return entries.size();
	}

	@Override
	public Object getItem(int pos) {
		Entry e = null;
		if (entries == null || pos < 0 || pos >= entries.size())
			return e;
		e = entries.get(pos);
		return e;
	}

	@Override
	public long getItemId(int pos) {
		return pos;
	}
	
	private int getLayoutId(Entry e) {
		int rv = 0;
		switch (e.itemType) {
//			case Constants.TYPE_HEADER:
//				if (e.data == 2)
//					rv = R.layout.listitem_hdr_2;
//				else
//					rv = R.layout.listitem_hdr;
//			break;
			case Gateway:
				rv = R.layout.listitem_3;
			break;
			case Group:
			case Host:
				rv = R.layout.listitem_2;
			break;
			case NodeManager:
				rv = android.R.layout.simple_list_item_1;
			break;
			default:
				rv = R.layout.listitem_2;
		}
		return rv;
	}

	@Override
	public View getView(int pos, View view, ViewGroup parent) {
		View rv = view;
		Entry e = (Entry)getItem(pos);
		if (e != null) {
			if (rv == null)
				rv = inflater.inflate(getLayoutId(e), null);
			TextView txt01 = (TextView)rv.findViewById(android.R.id.text1);
			TextView txt02 = (TextView)rv.findViewById(android.R.id.text2);
			ImageView img01 = (ImageView)rv.findViewById(android.R.id.icon);
			switch (e.itemType) {
//				case Constants.TYPE_HEADER:
//					txt01.setText(e.name);
//				break;
				case NodeManager:
					//node managers are not actually displayed by the adapter; this is a special entry used to provide status info
					txt01.setText(e.name);
					if (e.data != -1 && txt02 != null) {
						txt02.setText(buildDetail(e));
						if (img01 != null)
							img01.setImageBitmap(bmpFactory.get(e.itemType));
					}
				break;
				case Gateway:
				case Group:
				case Host:
					txt01.setText(e.name);
					if (txt02 != null) {
						txt02.setText(buildDetail(e));
					}
					if (img01 != null)
						img01.setImageBitmap(bmpFactory.get(e.itemType));
				break;
			}
		}
		return rv;
	}
	
	private String buildDetail(Entry e) {
		StringBuilder sb = new StringBuilder();
		if (e == null)
			return sb.toString();
		sb.append("id: ").append(e.id);
		if (e.itemType == EntityType.Group || e.itemType == EntityType.Host) {
			sb.append(", ").append(e.data).append(" instance");
			if (e.data != 1)
				sb.append("s");
		}
		return sb.toString();
	}
}
