package com.axway.ate.fragment;

import java.util.Map;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;

import com.axway.ate.R;
import com.axway.ate.adapter.TagListAdapter;
import com.axway.ate.fragment.EditTagDialog.EditTagListener;

abstract public class TagAwareFragment extends EditFragment implements OnItemClickListener, EditTagListener{

	private ListView listTags;
	private ImageButton btnAddTag;
	
	protected int curTagPos;
	
	public TagAwareFragment() {
		super();
		curTagPos = -1;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = super.onCreateView(inflater, container, savedInstanceState);
		listTags = (ListView)rv.findViewById(R.id.tag_list);
		listTags.setOnItemClickListener(this);
		listTags.setOnCreateContextMenuListener(this);
		btnAddTag = (ImageButton)rv.findViewById(R.id.action_add_tag);
		if (btnAddTag != null)
			btnAddTag.setOnClickListener(this);
		if (savedInstanceState != null)
			curTagPos = savedInstanceState.getInt("curTagPos", -1);
		return rv;
	}

	protected ListView getTagList() {
		return listTags;
	}

	protected void displayTags() {
		if (listTags == null)
			return;
		Map<String, String> tags = getObjectTags();
		if (tags == null)
			listTags.setAdapter(null);
		else
			listTags.setAdapter(new TagListAdapter(getActivity(), tags));
	}
	
	protected Map.Entry<String, String> getTagAtPosition(int pos) {
		if (listTags == null)
			return null;
		return (Map.Entry<String, String>)listTags.getItemAtPosition(pos);
	}
	
	@Override
	public void onItemClick(AdapterView<?> listView, View view, int pos, long id) {
		if (listView.getId() == R.id.tag_list) {
			Map.Entry<String, String> tag = getTagAtPosition(pos);
			if (tag != null)
				editTag(tag);
		}
	}
	
	protected void editTag(Map.Entry<String, String> tag) {
		EditTagDialog dlg = new EditTagDialog();
		Bundle args = new Bundle();
		String key = "";
		String val = "";
		if (tag != null) {
			key = tag.getKey();
			val = tag.getValue();
		}
		if (TextUtils.isEmpty(key))
			args.putString(Intent.EXTRA_TITLE, "Add Tag");
		else
			args.putString(Intent.EXTRA_TITLE, "Edit Tag " + key);
		args.putString(Intent.EXTRA_UID, key);
		args.putInt(AlertDialogFragment.EXTRA_LAYOUT, R.layout.edit_tag);
		args.putString(Intent.EXTRA_SUBJECT, val);
		dlg.setOnChangeListener(this);
		dlg.setArguments(args);
		dlg.show(getFragmentManager(), "editTag");
	}

	abstract protected Map<String, String> getObjectTags();
	
	@Override
	public void onTagChanged(String key, String value, int action) {
		Map<String, String> tags = getObjectTags();
		if (tags == null) {
			listTags.setAdapter(null);
			return;
		}
		tags.remove(key);
		tags.put(key, value);
		displayTags();
//		listTags.setAdapter(new TagListAdapter(getActivity(), tags));
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo cmi = (AdapterContextMenuInfo)menuInfo;
		curTagPos = cmi.position;
		final Map.Entry<String, String> e = getTagAtPosition(curTagPos);
		if (e == null)
			return;
		menu.setHeaderTitle("Tag: " + e.getKey());
		menu.add(0, R.id.action_delete, 1, "Delete");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		boolean rv = true;
		switch (item.getItemId()) {
			case R.id.action_delete:
				confirmDeleteTag();
			break;
			default:
				rv = super.onContextItemSelected(item);
		}
		return rv;
	}

	private void confirmDeleteTag() {
		if (curTagPos < 0)
			return;
		final Map.Entry<String, String> e = getTagAtPosition(curTagPos);
		if (e == null)
			return;
		AlertDialogFragment dlg = new AlertDialogFragment();
		Bundle args = new Bundle();
		args.putString(Intent.EXTRA_TITLE, "Confirm Delete");
		args.putString(Intent.EXTRA_TEXT, "Touch OK to delete tag " + e.getKey());
		dlg.setOnPositive(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				deleteTag(e);
				curTagPos = -1;
			}
		});
		dlg.setOnNegative(AlertDialogFragment.NOOP_LISTENER);
		dlg.setArguments(args);
		dlg.show(getFragmentManager(), "delTag");
	}
	
	private void deleteTag(Map.Entry<String, String> tag) {
		if (tag == null)
			return;
		Map<String, String> tags = null;
		tags = getObjectTags();
		if (tags != null && tags.containsKey(tag.getKey())) {
			tags.remove(tag.getKey());
			displayTags();
//			listTags.setAdapter(new TagListAdapter(getActivity(), tags));
		}
	}

//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		super.onCreateOptionsMenu(menu, inflater);
//		inflater.inflate(R.menu.tag_menu, menu);
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		boolean rv = true;
//		switch (item.getItemId()) {
//			case R.id.action_add_tag:
//				editTag(null);
//			break;
//			default:
//				rv = super.onOptionsItemSelected(item);
//		}
//		return rv;
//	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.action_add_tag) {
			editTag(null);
		}
		else
			super.onClick(v);
	}
}
