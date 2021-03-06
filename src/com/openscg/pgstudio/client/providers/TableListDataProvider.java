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
import com.openscg.pgstudio.client.PgStudioService;
import com.openscg.pgstudio.client.PgStudioServiceAsync;
import com.openscg.pgstudio.client.PgStudio.ITEM_TYPE;
import com.openscg.pgstudio.client.messages.TablesJsObject;
import com.openscg.pgstudio.client.models.DatabaseObjectInfo;
import com.openscg.pgstudio.client.models.TableInfo;
import com.openscg.pgstudio.client.models.TableInfo.TABLE_TYPE;

	public class TableListDataProvider extends AsyncDataProvider<TableInfo> implements ModelListProvider 
	{
			private List<TableInfo> tableList = new ArrayList<TableInfo>();

			private DatabaseObjectInfo schema = null;
			
			private final PgStudioServiceAsync studioService = GWT.create(PgStudioService.class);

			private final TableListDataProvider me = this;
			
			public void setSchema(DatabaseObjectInfo schema) {
				this.schema = schema;
				getData();
			}

			public List<TableInfo> getList()	{
				return tableList;
			}

			public void refresh() {
				getData();				
			}

			@Override
			protected void onRangeChanged(HasData<TableInfo> display)
			{
				getData();
				
				int start = display.getVisibleRange().getStart();
		        int end = start + display.getVisibleRange().getLength();
		        end = end >= tableList.size() ? tableList.size() : end;
		        List<TableInfo> sub = tableList.subList(start, end);
		        updateRowData(start, sub);
		        
			}

	private void getData() {
		if (schema != null) {
			studioService.getList(PgStudio.getToken(), schema.getId(),
					ITEM_TYPE.TABLE, new AsyncCallback<String>() {
						public void onFailure(Throwable caught) {
							tableList.clear();
							// Show the RPC error message to the user
							Window.alert(caught.getMessage());
						}

						public void onSuccess(String result) {
							tableList = new ArrayList<TableInfo>();

							JsArray<TablesJsObject> tables = json2Messages(result);

							if (tables != null) {
								tableList.clear();
								for (int i = 0; i < tables.length(); i++) {
									TablesJsObject table = tables.get(i);
									tableList.add(msgToColumnInfo(table));
								}
							}

							updateRowCount(tableList.size(), true);
							updateRowData(0, tableList);
						}
					});
		}
	}

			private TableInfo msgToColumnInfo(TablesJsObject msg) {
				int id = Integer.parseInt(msg.getId());

				TableInfo table = new TableInfo(schema.getId(), id, msg.getName());

				table.setComment(msg.getComment());
				
				if (msg.getTableType().equalsIgnoreCase("H")) {
					table.setType(TABLE_TYPE.HASH_PARTITIONED);
				} else if (msg.getTableType().equalsIgnoreCase("R")){
					table.setType(TABLE_TYPE.REPLICATED);					
				} else {
					table.setType(TABLE_TYPE.UNKNOWN);
				}
				
				if (msg.getRowSecurity().equalsIgnoreCase("true")) {
					table.setHasRowSecurity(true);
				} else {
					table.setHasRowSecurity(false);
				}

				if (msg.getForceRowSecurity().equalsIgnoreCase("true")) {
					table.setForceRowSecurity(true);
				} else {
					table.setForceRowSecurity(false);
				}

				return table;
			}

			private static final native JsArray<TablesJsObject> json2Messages(
					String json)
			/*-{ 
			  	return eval(json); 
			}-*/;

	}