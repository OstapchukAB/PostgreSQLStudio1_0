/*
 * PostgreSQL Studio
 */
package com.openscg.pgstudio.client.panels.navigation;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.openscg.pgstudio.client.PgStudio.ITEM_TYPE;
import com.openscg.pgstudio.client.Resources;
import com.openscg.pgstudio.client.PgStudio;
import com.openscg.pgstudio.client.models.DatabaseObjectInfo;
import com.openscg.pgstudio.client.models.SequenceInfo;
import com.openscg.pgstudio.client.panels.popups.AddSequencePopUp;
import com.openscg.pgstudio.client.panels.popups.DropItemPopUp;
import com.openscg.pgstudio.client.panels.popups.PopUpException;
import com.openscg.pgstudio.client.providers.SequenceListDataProvider;

public class SequencesPanel extends Composite implements MenuPanel {

	private static final Resources Images =  GWT.create(Resources.class);
	
	private static interface GetValue<C> {
	    C getValue(SequenceInfo object);
	  }

	private DatabaseObjectInfo schema = null;

	private DataGrid<SequenceInfo> dataGrid;
    private SequenceListDataProvider dataProvider = new SequenceListDataProvider();
	
    private final SingleSelectionModel<SequenceInfo> selectionModel = 
    	new SingleSelectionModel<SequenceInfo>(SequenceInfo.KEY_PROVIDER);
        
	private final PgStudio main;
	
	private VerticalPanel panel;
	
	private boolean isFirst = true;
	
	public void setSchema(DatabaseObjectInfo schema) {
		this.schema = schema;
		//dataProvider.setSchema(schema);
	}
	
	public SequencesPanel(PgStudio main) {
		this.main = main;
		
		panel = new VerticalPanel();
		panel.setWidth("95%");

		panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

		panel.add(getButtonBar());
		
		dataGrid = new DataGrid<SequenceInfo>(PgStudio.MAX_PANEL_ITEMS, SequenceInfo.KEY_PROVIDER);
		dataGrid.setHeight(PgStudio.LEFT_PANEL_HEIGHT);
//		panel.add(getSequenceList());		
//		
//		dataGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<SequenceInfo>() {
//			@Override
//			public void onCellPreview(CellPreviewEvent<SequenceInfo> event) {
//				if (BrowserEvents.CLICK.equals(event.getNativeEvent().getType())) {
//					if (dataGrid.getRowCount() == 1) {
//						SequenceInfo i = dataProvider.getList().get(0);
//
//						if (dataGrid.getSelectionModel().isSelected(i)) {
//							selectFirst();
//						}
//					}
//	            }
//			}
//		});

		initWidget(panel);
	}

	private Widget getButtonBar() {
		HorizontalPanel bar = new HorizontalPanel();
		
		PushButton refresh = getRefreshButton();
		PushButton drop = getDropButton();
		PushButton create = getCreateButton();
		
		bar.add(refresh);
		bar.add(drop);
		bar.add(create);
		
		return bar.asWidget();
	}
	
	private PushButton getRefreshButton() {
		PushButton button = new PushButton(new Image(Images.refresh()));
		button.setTitle("Refresh");

		button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				refresh();				
			}			
		});

		return button;
	}
	
	private PushButton getDropButton() {
		PushButton button = new PushButton(new Image(Images.drop()));
		button.setTitle("Drop Sequence");
		button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if(selectionModel.getSelectedObject() != null && !"".equals(selectionModel.getSelectedObject().getName())){
					DropItemPopUp pop = new DropItemPopUp();
					pop.setSelectionModel(selectionModel);
					pop.setDataProvider(dataProvider);
					pop.setSchema(schema);
					pop.setItemType(ITEM_TYPE.SEQUENCE);
					pop.setItem(selectionModel.getSelectedObject().getName());
					try {
						pop.getDialogBox();
					} catch (PopUpException caught) {
						Window.alert(caught.getMessage());
					}
				}
			}			
		});
		return button;
	}

	private PushButton getCreateButton() {
		PushButton button = new PushButton(new Image(Images.create()));
		button.setTitle("Create Sequence");
		
		button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				AddSequencePopUp pop = new AddSequencePopUp();
				pop.setSelectionModel(selectionModel);
				pop.setDataProvider(dataProvider);
				pop.setSchema(schema);
				try {
					pop.getDialogBox();
				} catch (PopUpException caught) {
					Window.alert(caught.getMessage());
				}
			}
		});

		return button;
	}

	private Widget getSequenceList() {
//		dataGrid = new DataGrid<SequenceInfo>(PgStudio.MAX_PANEL_ITEMS, SequenceInfo.KEY_PROVIDER);
//		dataGrid.setHeight(PgStudio.LEFT_PANEL_HEIGHT);

	
		Column<SequenceInfo, ImageResource> icon = addColumn(new ImageResourceCell(), "", new GetValue<ImageResource>() {
	        public ImageResource getValue(SequenceInfo column) {
        		return Images.sequences();
	        }
	      }, null);

		Column<SequenceInfo, String> seqName = addColumn(new TextCell(), "", new GetValue<String>() {
	        public String getValue(SequenceInfo column) {
	          return column.getName();
	        }
	      }, null);

		dataGrid.setColumnWidth(icon, "35px");
		icon.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		
		dataProvider.addDataDisplay(dataGrid);

		dataGrid.setSelectionModel(selectionModel);
		selectionModel.addSelectionChangeHandler((Handler) main.getSelectionChangeHandler());

		return dataGrid.asWidget();
	}
	
	private <C> Column<SequenceInfo, C> addColumn(Cell<C> cell, String headerText,
		      final GetValue<C> getter, FieldUpdater<SequenceInfo, C> fieldUpdater) {
		    Column<SequenceInfo, C> column = new Column<SequenceInfo, C>(cell) {
		      @Override
		      public C getValue(SequenceInfo object) {
		        return getter.getValue(object);
		      }
		    };
		    column.setFieldUpdater(fieldUpdater);

		    dataGrid.addColumn(column, headerText);
		    return column;
	}

	public void refresh() {
		dataProvider.setSchema(schema);
		
		if(isFirst){
			isFirst = false;
			panel.add(getSequenceList());		
			
			dataGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<SequenceInfo>() {
				@Override
				public void onCellPreview(CellPreviewEvent<SequenceInfo> event) {
					if (BrowserEvents.CLICK.equals(event.getNativeEvent().getType())) {
						if (dataGrid.getRowCount() == 1) {
							SequenceInfo i = dataProvider.getList().get(0);

							if (dataGrid.getSelectionModel().isSelected(i)) {
								selectFirst();
							}
						}
		            }
				}
			});
		}
		
		selectFirst();
	}
	
	@Override
	public Boolean selectFirst() {
		if (dataProvider != null) {
			if (!dataProvider.getList().isEmpty()) {
				SequenceInfo i = dataProvider.getList().get(0);
				dataGrid.getSelectionModel().setSelected(i, true);
				main.setSelectedItem(i);
				return true;
			}
		}
		
		return false;
	}

}
