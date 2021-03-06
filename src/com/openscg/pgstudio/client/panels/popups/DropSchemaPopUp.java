/*
 * PostgreSQL Studio
 */
package com.openscg.pgstudio.client.panels.popups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.openscg.pgstudio.client.PgStudio;
import com.openscg.pgstudio.client.PgStudioService;
import com.openscg.pgstudio.client.PgStudioServiceAsync;
import com.openscg.pgstudio.client.handlers.UtilityCommandAsyncCallback;
import com.openscg.pgstudio.client.models.DatabaseObjectInfo;

public class DropSchemaPopUp implements StudioPopUp {
	
	private final static String WARNING_MSG = 
			"This will permanently delete this schema. Are you sure you want to continue?";
	
	private final PgStudioServiceAsync studioService = GWT.create(PgStudioService.class);

	final DialogBox dialogBox = new DialogBox();

    private DatabaseObjectInfo schema = null;
    
    private CheckBox cascadeBox;
    
	@Override
	public DialogBox getDialogBox() throws PopUpException {
		if (schema == null)
			throw new PopUpException("Schema is not set");
		
		dialogBox.setWidget(getPanel());
		
		dialogBox.setGlassEnabled(true);
		dialogBox.center();

		return dialogBox;
	}

	public void setSchema(DatabaseObjectInfo schema) {
		this.schema = schema;
	}
	
	private VerticalPanel getPanel(){
		VerticalPanel panel = new VerticalPanel();
		panel.setStyleName("StudioPopup");

		VerticalPanel info = new VerticalPanel();
		info.setSpacing(10);
		
		Label lbl = new Label();
		lbl.setStyleName("StudioPopup-Msg-Strong");
		lbl.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);	
		
		String title = "DROP " + schema.getName();
		lbl.setText(title);
		
		HorizontalPanel warningPanel = new HorizontalPanel();
		Image icon = new Image(PgStudio.Images.warning());
		icon.setWidth("110px");
		
		VerticalPanel detailPanel = new VerticalPanel();
		
		Label lblWarning = new Label();
		lblWarning.setStyleName("StudioPopup-Msg");
		lblWarning.setText(WARNING_MSG);

		HorizontalPanel cascadePanel = new HorizontalPanel();
		cascadePanel.setSpacing(5);
		
		SimplePanel cascadeBoxPanel = new SimplePanel();
		cascadeBoxPanel.setStyleName("roundedCheck");
		
		cascadeBox = new CheckBox();
		cascadeBoxPanel.add(cascadeBox);

		Label lblCascade = new Label();
		lblCascade.setStyleName("StudioPopup-Msg");
		lblCascade.setText("Cascade");

		cascadePanel.add(cascadeBoxPanel);
		cascadePanel.add(lblCascade);
		cascadePanel.setCellVerticalAlignment(cascadeBoxPanel, HasVerticalAlignment.ALIGN_MIDDLE);
		cascadePanel.setCellVerticalAlignment(lblCascade, HasVerticalAlignment.ALIGN_MIDDLE);
		
		detailPanel.add(lblWarning);
		detailPanel.add(cascadePanel);
		
		warningPanel.add(icon);
		warningPanel.add(detailPanel);
		
		info.add(lbl);
		info.add(warningPanel);
		
		panel.add(info);
		
		Widget buttonBar = getButtonPanel(); 
		panel.add(buttonBar);
		panel.setCellHorizontalAlignment(buttonBar, HasHorizontalAlignment.ALIGN_CENTER);
		
		return panel;
	}
	
	private Widget getButtonPanel(){
		HorizontalPanel bar = new HorizontalPanel();
		bar.setHeight("50px");
		bar.setSpacing(10);
		
		Button yesButton = new Button("Yes");
		Button noButton = new Button("No");
		
		bar.add(yesButton);
		bar.add(noButton);
		
		bar.setCellHorizontalAlignment(yesButton, HasHorizontalAlignment.ALIGN_CENTER);
		bar.setCellHorizontalAlignment(noButton, HasHorizontalAlignment.ALIGN_CENTER);

		yesButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
					boolean cascade = cascadeBox.getValue();
					
					UtilityCommandAsyncCallback ac = new UtilityCommandAsyncCallback(
							dialogBox, null);
					ac.setAutoRefresh(false);
					ac.setShowResultOutput(false);

					studioService.dropSchema(PgStudio.getToken(), schema.getName(), cascade, ac);
			}
		});

		noButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				dialogBox.hide(true);
			}
		});

		return bar.asWidget();
	}	
}
