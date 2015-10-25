package se.ade.autoproxywrapper.gui.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.apache.commons.lang3.StringUtils;
import se.ade.autoproxywrapper.events.EventBus;
import se.ade.autoproxywrapper.events.RestartEvent;
import se.ade.autoproxywrapper.model.ForwardProxy;

import java.util.List;

import static se.ade.autoproxywrapper.Config.config;
import static se.ade.autoproxywrapper.Config.save;

public class ProxiesController {

    @FXML
    public ListView<ForwardProxy> hostList;

    @FXML
    public TextField newHostName;

    @FXML
    public TextField newPort;

    @FXML
    public Button saveButton;

    private ForwardProxy selectedProxy;

    private ObservableList<ForwardProxy> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        hostList.setPlaceholder(getPlaceholder());
        hostList.getSelectionModel().selectedItemProperty().addListener(getForwardProxyChangeListener());
        hostList.setCellFactory(param -> new ForwardProxyListCell());
        Bindings.bindContent(hostList.getItems(), items);

        items.addAll(config().getForwardProxies());
    }

    @FXML
    public void saveForwardProxy(Event event) {
        if (event instanceof KeyEvent && ((KeyEvent) event).getCode() != KeyCode.ENTER) {
            return;
        }
        if (!validateInput()) {
            return;
        }
        selectedProxy.setHost(newHostName.getText());
        selectedProxy.setPort(Integer.parseInt(newPort.getText()));
        config().setForwardProxies(items);

        save();
        hostList.refresh();

        EventBus.get().post(new RestartEvent());
    }

    private boolean validateInput() {
        if (newHostName.getText().equals("")) {
            if (!newHostName.getStyleClass().contains("invalid-field")) {
                newHostName.getStyleClass().add("invalid-field");
            }
            return false;
        }
        newHostName.getStyleClass().remove("invalid-field");
        if (newPort.getText().equals("") || !StringUtils.isNumeric(newPort.getText()) || Integer.parseInt(newPort.getText()) > Short.MAX_VALUE) {
            if (!newPort.getStyleClass().contains("invalid-field")) {
                newPort.getStyleClass().add("invalid-field");
            }
            return false;
        }
        newPort.getStyleClass().remove("invalid-field");
        return true;
    }

    private ChangeListener<ForwardProxy> getForwardProxyChangeListener() {
        return (observable, oldValue, newValue) -> {
            selectedProxy = newValue;
            newHostName.setText(selectedProxy.getHost());
            newPort.setText(Integer.toString(selectedProxy.getPort()));
            newHostName.setDisable(false);
            newPort.setDisable(false);
            saveButton.setDisable(false);
        };
    }

    private Label getPlaceholder() {
        Label label = new Label("No proxies configured. Click here to add one.");
        label.setOnMouseClicked(event -> {
            ForwardProxy newForwardProxy = new ForwardProxy("", 0);
            items.add(newForwardProxy);
            hostList.getSelectionModel().select(newForwardProxy);
            newHostName.requestFocus();
        });
        return label;
    }

    public class ForwardProxyListCell extends ListCell<ForwardProxy> {

        @Override
        protected void updateItem(ForwardProxy item, boolean empty) {
            ContextMenu contextMenu = new ContextMenu();
            if (empty) {
                setText("");

                MenuItem addMenuItem = new MenuItem("Add", new ImageView(getClass().getResource("/icon/Plus.png").toExternalForm()));
                addMenuItem.setOnAction(getAddEvent());
                contextMenu.getItems().add(addMenuItem);
            } else {
                setText(item.getHost() + ":" + item.getPort());

                MenuItem removeMenuItem = new MenuItem("Remove", new ImageView(getClass().getResource("/icon/Minus.png").toExternalForm()));
                removeMenuItem.setOnAction(getRemoveEvent());
                contextMenu.getItems().add(removeMenuItem);
            }
            setContextMenu(contextMenu);
            super.updateItem(item, empty);
        }

        private EventHandler<ActionEvent> getAddEvent() {
            return event -> {
                ForwardProxy newForwardProxy = new ForwardProxy("", 0);
                items.add(newForwardProxy);
                hostList.getSelectionModel().select(newForwardProxy);
                newHostName.requestFocus();
            };
        }

        private EventHandler<ActionEvent> getRemoveEvent() {
            return event -> {
                List<ForwardProxy> forwardProxies = config().getForwardProxies();
                if(forwardProxies.isEmpty()) {
                    items.clear();
                } else {
                    for (ForwardProxy proxy : forwardProxies) {
                        if (proxy.equals(hostList.getSelectionModel().getSelectedItem())) {
                            items.remove(proxy);
                            config().setForwardProxies(items);
                            save();
                            EventBus.get().post(new RestartEvent());
                            break;
                        }
                    }
                }
                hostList.refresh();
            };
        }
    }
}
