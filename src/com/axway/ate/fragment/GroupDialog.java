package com.axway.ate.fragment;

import java.util.Map;

import com.axway.ate.Constants;
import com.axway.ate.DomainHelper;
import com.axway.ate.R;
import com.axway.ate.adapter.TagListAdapter;
import com.axway.ate.util.Utilities;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

public class GroupDialog extends TagAwareDialog {

	public interface GroupListener {
		public void onGroupChanged(Bundle b);
	}

	private GroupListener listener;
	private Group grp;
	
	public GroupDialog() {
		super();
		listener = null;
		grp = null;
	}

	public void setOnChangeListener(GroupListener newVal) {
		listener = newVal;
	}
	
	private DialogInterface.OnClickListener onYes = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			String name = editName.getText().toString();
			if (isValidName(name)) {			
				Bundle data = new Bundle();
				data.putInt(Constants.EXTRA_ACTION, action);
				data.putString(Intent.EXTRA_TEXT, name);
				grp.setName(name);
				data.putString(Constants.EXTRA_JSON_ITEM, DomainHelper.getInstance().toJson(grp).toString());
				if (action == R.id.action_add) {
				}
				else
					data.putString(Intent.EXTRA_UID, (String)editName.getTag());
				if (listener != null && !TextUtils.isEmpty(name))
					listener.onGroupChanged(data);
			}
		}		
	};

	@Override
	protected int getLayoutId() {
		return R.layout.edit_grp;
	}

	@Override
	protected EntityType getItemType() {
		return EntityType.Group;
	}

	@Override
	protected OnClickListener createOnYes() {
		return onYes;
	}

	@Override
	protected void setupView(View dlgView) {
		super.setupView(dlgView);
		if (itemJson != null)
			grp = DomainHelper.getInstance().groupFromJson(itemJson);
		displayTags();
	}

	@Override
	protected Map<String, String> getObjectTags() {
		if (grp == null)
			return null;
		return grp.getTags();
	}
}