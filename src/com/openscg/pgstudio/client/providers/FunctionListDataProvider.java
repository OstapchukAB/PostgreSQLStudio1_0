/*
 * PostgreSQL Studio
 */
package com.openscg.pgstudio.client.providers;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.openscg.pgstudio.client.PgStudio;
import com.openscg.pgstudio.client.PgStudio.ITEM_TYPE;
import com.openscg.pgstudio.client.PgStudioService;
import com.openscg.pgstudio.client.PgStudioServiceAsync;
import com.openscg.pgstudio.client.messages.FunctionsJsObject;
import com.openscg.pgstudio.client.models.DatabaseObjectInfo;
import com.openscg.pgstudio.client.models.FunctionInfo;

public class FunctionListDataProvider extends AsyncDataProvider<FunctionInfo>
		implements ModelListProvider {
	private List<FunctionInfo> funcList = new ArrayList<FunctionInfo>();

	private DatabaseObjectInfo schema = null;
	
	private boolean isRestricted;

	public void setRestricted(boolean isRestricted) {
		this.isRestricted = isRestricted;
	}

	private final PgStudioServiceAsync studioService = GWT
			.create(PgStudioService.class);

	public void setSchema(DatabaseObjectInfo schema) {
		this.schema = schema;
		getData();
	}

	public List<FunctionInfo> getList()	{
		return funcList;
	}

	public void refresh() {
		getData();
	}

	@Override
	protected void onRangeChanged(HasData<FunctionInfo> display) {
		getData();

		int start = display.getVisibleRange().getStart();
		int end = start + display.getVisibleRange().getLength();
		end = end >= funcList.size() ? funcList.size() : end;
		List<FunctionInfo> sub = funcList.subList(start, end);
		updateRowData(start, sub);

	}

	private void getData() {
		if (schema != null) {
			if(!isRestricted){
				studioService.getFunctionFullList(PgStudio.getToken(), schema.getId(),
					ITEM_TYPE.FUNCTION, new AsyncCallback<String>() {
						public void onFailure(Throwable caught) {
							funcList.clear();
							// Show the RPC error message to the user
							Window.alert(caught.getMessage());
						}

						public void onSuccess(String result) {
							funcList = new ArrayList<FunctionInfo>();

							JsArray<FunctionsJsObject> funcs = json2Messages(result);

							if (funcs != null) {
								funcList.clear();

								for (int i = 0; i < funcs.length(); i++) {
									FunctionsJsObject func = funcs.get(i);
									funcList.add(msgToInfo(func, schema.getId()));
								}
							}

							updateRowCount(funcList.size(), true);
							updateRowData(0, funcList);

						}
					});
			}else {
				studioService.getList(PgStudio.getToken(), schema.getId(),
					ITEM_TYPE.FUNCTION, new AsyncCallback<String>() {
						public void onFailure(Throwable caught) {
							funcList.clear();
							// Show the RPC error message to the user
							Window.alert(caught.getMessage());
						}

						public void onSuccess(String result) {
							funcList = new ArrayList<FunctionInfo>();

							JsArray<FunctionsJsObject> funcs = json2Messages(result);

							if (funcs != null) {
								funcList.clear();

								for (int i = 0; i < funcs.length(); i++) {
									FunctionsJsObject func = funcs.get(i);
									funcList.add(msgToInfo(func, schema.getId()));
								}
							}

							updateRowCount(funcList.size(), true);
							updateRowData(0, funcList);

						}
					});
			}
		}
	}

	public static final FunctionInfo msgToInfo(FunctionsJsObject msg, int schema) {
		int id = Integer.parseInt(msg.getId());

		FunctionInfo func = new FunctionInfo(schema, id, msg.getName());
		func.setIdentity(msg.getIdentity());

		return func;
	}

	public static final native JsArray<FunctionsJsObject> json2Messages(
			String json)
	/*-{
		return eval(json);
	}-*/;

}
