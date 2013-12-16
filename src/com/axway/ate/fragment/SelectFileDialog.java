package com.axway.ate.fragment;

import java.util.List;

import com.axway.ate.Constants;
import com.axway.ate.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class SelectFileDialog extends DialogFragment implements OnItemClickListener {

	public static final String EXTRA_ACTION = "action";
	
	private static final String DEF_TITLE = "Select File";
	
	private DialogInterface.OnClickListener onPositive;
	
	public interface Listener {
		public void onFileSelected(String fname);
	}
	
	private int posResId;
	private int negResId;
	private String selectedFile;
	private Listener listener;
	private ListView listView;
	private View ctr01;
	private View ctr02;
	private EditText editFname;
	private int action;
	
	public SelectFileDialog() {
		super();
		onPositive = null;
		posResId = android.R.string.ok;
		negResId = android.R.string.cancel;
		selectedFile = null;
		listener = null;
		listView = null;
		action = 0;
	}
	
	public void setListener(Listener l) {
		this.listener = l;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String t = DEF_TITLE;
		List<String> fnames = null;
		Bundle args = getArguments();
		if (args != null) {
			if (args.containsKey(Intent.EXTRA_TITLE))
				t = args.getString(Intent.EXTRA_TITLE);
			if (args.containsKey(Constants.EXTRA_JSON_ITEM))
				fnames = args.getStringArrayList(Constants.EXTRA_JSON_ITEM);
			if (args.containsKey(Constants.EXTRA_ACTION))
				action = args.getInt(Constants.EXTRA_ACTION);
		}
		AlertDialog.Builder bldr = new AlertDialog.Builder(getActivity());
    	Activity a = getActivity();
    	LayoutInflater inflater = LayoutInflater.from(a);
    	View dlgView = inflater.inflate(R.layout.file_dlg, null);
		editFname = (EditText)dlgView.findViewById(android.R.id.text1);
    	ctr01 = dlgView.findViewById(R.id.container01);
    	ctr02 = dlgView.findViewById(R.id.container02);
    	if (action == R.id.action_save_to_disk)
    		t = "Save Topology To File";
    	else if (action == R.id.action_load_from_disk)
    		t = "Load Topology From File";
    	if (action == R.id.action_load_from_disk) {
    		ctr01.setVisibility(View.GONE);
    		editFname = null;
    	}
    	else {
    		ctr01.setVisibility(View.VISIBLE);
    		editFname = (EditText)dlgView.findViewById(android.R.id.text1);
    	}
    	listView = (ListView)dlgView.findViewById(android.R.id.list);
    	if (fnames.size() == 0) {
    		ctr02.setVisibility(View.GONE);
    	}
    	else {
    		ctr02.setVisibility(View.VISIBLE);
    		listView.setAdapter(new ArrayAdapter<String>(a, android.R.layout.simple_list_item_1, fnames));
    		listView.setOnItemClickListener(this);
    	}
		bldr.setTitle(t)
			.setView(dlgView);
		onPositive = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (listener != null) {
					String fname = editFname.getText().toString();
					if (!TextUtils.isEmpty(fname))
						listener.onFileSelected(fname);
				}
			}
		};
		if (action == R.id.action_save_to_disk)
			bldr.setPositiveButton(posResId, onPositive);
		bldr.setNegativeButton(negResId, AlertDialogFragment.NOOP_LISTENER);
		return bldr.create();
	}

	public DialogInterface.OnClickListener getOnPositive() {
		return onPositive;
	}

	public void setOnPositive(DialogInterface.OnClickListener onPositive) {
		this.onPositive = onPositive;
	}

	public void setOnPositive(DialogInterface.OnClickListener onPositive, int resId) {
		setOnPositive(onPositive);
		posResId = resId;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View view, int pos, long id) {
		selectedFile = (String)listView.getItemAtPosition(pos);
		if (listener != null && selectedFile != null) {
			if (action == R.id.action_load_from_disk) {
				listener.onFileSelected(selectedFile);
				dismiss();
			}
			else if (action == R.id.action_save_to_disk) {
				editFname.setText(selectedFile);
			}
		}
	}
}
