package com.hubachov.client.element.table;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.Record;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.grid.*;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.hubachov.client.element.form.UserForm;
import com.hubachov.client.model.User;
import com.hubachov.client.service.RoleServiceAsync;
import com.hubachov.client.service.UserServiceAsync;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UserTable extends LayoutContainer {
    private final UserServiceAsync userServiceAsync;
    private final RoleServiceAsync roleServiceAsync;
    private Grid<User> grid;
    private List<ColumnConfig> columns;
    private ListStore<User> store;
    private BasePagingLoader<PagingLoadResult<User>> loader;
    private RpcProxy<BasePagingLoadResult<User>> proxy;
    private CheckBoxSelectionModel plugin = new CheckBoxSelectionModel<User>();
    private ContentPanel view = new ContentPanel();

    public UserTable(UserServiceAsync userServiceAsync, RoleServiceAsync roleServiceAsync) {
        this.userServiceAsync = userServiceAsync;
        this.roleServiceAsync = roleServiceAsync;
    }

    @Override
    protected void onRender(Element parent, int index) {
        super.onRender(parent, index);
        initProxy();
        loader = new BasePagingLoader<PagingLoadResult<User>>(proxy);
        configureColumns();
        store = new ListStore<User>(loader);
        grid = new Grid<User>(store, new ColumnModel(columns));
        addGridOnAttachListener();
        attachToolbars();
        styleGrid();
        view.add(grid);
        add(view);
    }

    private void configureColumns() {
        columns = new ArrayList<ColumnConfig>();
        columns.add(plugin.getColumn());
        columns.add(new ColumnConfig("id", "Id", 30));
        columns.add(new ColumnConfig("login", "Login", 100));
        columns.add(new ColumnConfig("email", "Email", 100));
        columns.add(new ColumnConfig("firstName", "First Name", 100));
        columns.add(new ColumnConfig("lastName", "Last Name", 100));
        columns.add(new ColumnConfig("birthday", "Birthday", 100));
        columns.add(new ColumnConfig("role", "Role", 120));
        alignColumns(Style.HorizontalAlignment.LEFT);
    }

    private void initProxy() {
        proxy = new RpcProxy<BasePagingLoadResult<User>>() {
            @Override
            protected void load(Object config, AsyncCallback<BasePagingLoadResult<User>> callback) {
                userServiceAsync.getUsers((BasePagingLoadConfig) config, callback);
            }
        };
    }

    private void attachToolbars() {
        PagingToolBar pagingToolBar = new PagingToolBar(10);
        pagingToolBar.bind(loader);
        ToolBar toolBar = new ToolBar();
        view.setHeading("Editable User Grid");
        view.setFrame(true);
        view.setSize(800, 350);
        view.setLayout(new FitLayout());
        view.setBottomComponent(pagingToolBar);
        view.setTopComponent(toolBar);
        toolBar.add(makeNewEditUserBtn("New", "add-btn"));
        toolBar.add(makeNewEditUserBtn("Edit", "edit-btn"));
        toolBar.add(makeDeleteBtn());
    }

    private void styleGrid() {
        grid.setStripeRows(true);
        grid.addPlugin(plugin);
        grid.setSelectionModel(plugin);
        grid.getSelectionModel().bind(store);
        grid.setSize(700, 350);
    }

    private void addGridOnAttachListener() {
        grid.addListener(Events.Attach, new Listener<GridEvent<User>>() {
            @Override
            public void handleEvent(GridEvent<User> baseEvent) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        PagingLoadConfig config = new BasePagingLoadConfig();
                        config.setOffset(0);
                        config.setLimit(10);
                        Map<String, Object> state = grid.getState();
                        if (state.containsKey("offset")) {
                            int offset = (Integer) state.get("offset");
                            int limit = (Integer) state.get("limit");
                            config.setLimit(limit);
                            config.setOffset(offset);
                        }
                        if (state.containsKey("sortField")) {
                            config.setSortField((String) state.get("sortField"));
                            config.setSortDir(Style.SortDir.valueOf((String) state.get("sortDir")));
                        }
                        loader.load(config);
                    }
                });
            }
        });
    }

    private Button makeNewEditUserBtn(String name, String id) {
        Button button = new Button(name);
        button.setId(id);
        addAddEditButtonListener(button);
        return button;
    }

    private Button makeDeleteBtn() {
        Button button = new Button("Delete");
        addDeleteButtonListener(button);
        return button;
    }

    private void addAddEditButtonListener(final Button button) {
        button.addSelectionListener(new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
                if (button.getId().equals("edit-btn")) {
                    showUserFormWindow(grid.getSelectionModel().getSelectedItem());
                } else {
                    showUserFormWindow(null);
                }
            }
        });
    }

    private void addDeleteButtonListener(Button button) {
        button.addSelectionListener(new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
                final List<User> selectedUsers = plugin.getSelectedItems();
                if (!selectedUsers.isEmpty()) {
                    MessageBox.confirm("Delete?", "Are you sure?", new Listener<MessageBoxEvent>() {
                        @Override
                        public void handleEvent(MessageBoxEvent be) {
                            Button btn = be.getButtonClicked();
                            if (Dialog.YES.equalsIgnoreCase(btn.getItemId())) {
                                for (User user : selectedUsers) {
                                    userServiceAsync.remove(user, new DeleteUserAsyncCallback<Void>(grid));
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private void alignColumns(Style.HorizontalAlignment side) {
        Iterator<ColumnConfig> iterator = columns.iterator();
        while (iterator.hasNext()) {
            iterator.next().setAlignment(side);
        }
    }

    private void showUserFormWindow(User user) {
        view.setEnabled(false);
        com.extjs.gxt.ui.client.widget.Window window =
                new com.extjs.gxt.ui.client.widget.Window();
        window.setClosable(false);
        window.add(new UserForm(grid, user, userServiceAsync, roleServiceAsync, window, view));
        window.setWidth(350);
        window.show();
    }

}

class DeleteUserAsyncCallback<Void> implements AsyncCallback<Void> {
    private final Grid<User> grid;

    public DeleteUserAsyncCallback(Grid<User> grid) {
        this.grid = grid;
    }

    @Override
    public void onFailure(Throwable throwable) {
        Window.alert(throwable.getMessage());
    }

    @Override
    public void onSuccess(Void aVoid) {
        Info.display("Success", "Deleted");
        grid.getStore().getLoader().load();
    }
}