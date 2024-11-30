package notes;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ListView;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import jfxtras.scene.control.LocalDatePicker;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ChangeListener;
import javafx.beans.property.SimpleBooleanProperty;
import javax.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.TimerTask;
import java.util.Timer;

import org.springframework.dao.DataAccessException;


/*
 * Class builds the GUI for the app.
 * Gets the app's data from the database and shows in the reminders table.
 * Starts a Timer and schedules its reminder notification task.
 * Has functions to add, delete and update reminders.
 */
public class AppGui {


    private TableView<Reminder> table;
    private ListView<String> list;
    private LocalDatePicker localDatePicker;
    private Text actionStatus;
	
    private HBox hbPane;
    private Timer timer;
    private DataAccessException dataAccessException;

    private DataAccess dataAccess;
    private CheckRemindersTask remindersTask;
    private ReminderDialog reminderDialog;
	

    /*
     * Constructor.
     * Constructs the app's GUI.
     */
    public AppGui(DataAccess dataAccess, CheckRemindersTask remindersTask, ReminderDialog reminderDialog) {
	
        this.dataAccess = dataAccess;
        this.remindersTask = remindersTask;
        this.reminderDialog = reminderDialog;

        // List view and calendar
        list = new ListView<String>(ReminderGroup.getAsFormattedStrings());
        list.getSelectionModel().selectedIndexProperty().addListener(
                new ListSelectChangeListener());		
        localDatePicker = new LocalDatePicker();
		
        // VBox with list and calendar
        VBox vbox1 = new VBox(15);
        vbox1.getChildren().addAll(list, localDatePicker);

        // Buttons in a hbox
        Button newBtn = new Button("New");
        newBtn.setOnAction(actionEvent -> newReminderRoutine());
        Button updBtn = new Button("Update");
        updBtn.setOnAction(actionEvent -> updateReminderRoutine());
        Button delBtn = new Button("Delete");
        delBtn.setOnAction(actionEvent -> deleteReminderRoutine());
		
        HBox btnHb = new HBox(10);
        btnHb.getChildren().addAll(newBtn, updBtn, delBtn);
		
        // Table view, columns and properties (row and column)	
        table = new TableView<Reminder>();
		
        TableColumn<Reminder, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<Reminder, String>("name"));		
        nameCol.setMaxWidth(1f * Integer.MAX_VALUE * 45);  // 45% of table width
		
        TableViewRowAndCellFactories cellFactories = new TableViewRowAndCellFactories();

        TableColumn<Reminder, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setMaxWidth(1f * Integer.MAX_VALUE * 15);
        dateCol.setCellValueFactory(new PropertyValueFactory<Reminder, LocalDate>("date"));
        dateCol.setCellFactory(column -> cellFactories.getTableCellWithDateFormatting());	
		
        TableColumn<Reminder, LocalTime> timeCol = new TableColumn<>("Time");
        timeCol.setMaxWidth(1f * Integer.MAX_VALUE * 12);
        timeCol.setCellValueFactory(new PropertyValueFactory<Reminder, LocalTime>("time"));
        timeCol.setCellFactory(column -> cellFactories.getTableCellWithTimeFormatting());

        TableColumn<Reminder, Boolean> priorityCol = new TableColumn<>("Priority");
        priorityCol.setMaxWidth(1f * Integer.MAX_VALUE * 12);
        priorityCol.setCellValueFactory(new PropertyValueFactory<Reminder, Boolean>("priority"));
        priorityCol.setCellFactory(column -> {
            CheckBoxTableCell<Reminder, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            return cell;
        });

        TableColumn<Reminder, Boolean> completedCol = new TableColumn<>("Completed");
        completedCol.setMaxWidth(1f * Integer.MAX_VALUE * 15);
        completedCol.setCellValueFactory(new PropertyValueFactory<Reminder, Boolean>("completed"));
        completedCol.setCellFactory(column -> {
            CheckBoxTableCell<Reminder, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            return cell;
        });

        table.getColumns().addAll(nameCol, dateCol,timeCol, priorityCol, completedCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefWidth(600);
        table.setRowFactory(tableView -> cellFactories.getTooltipTableRow());

        // Table row item double-click mouse event
        table.setOnMousePressed(mouseEvent -> {
            if ((mouseEvent.isPrimaryButtonDown()) &&
                    (mouseEvent.getClickCount() == 2)) {
                updateReminderRoutine();
            }
        });

        // Status message text
        actionStatus = new Text();
        actionStatus.setFill(Color.FIREBRICK);

        // Vbox with buttons hbox, table view and status text
        VBox vbox2 = new VBox(15);
        vbox2.getChildren().addAll(btnHb, table, actionStatus);

        // Hbox with vbox1 and vbox2
        hbPane = new HBox(15);
        hbPane.setPadding(new Insets(15));
        hbPane.setAlignment(Pos.CENTER);
        hbPane.getChildren().addAll(vbox1, vbox2);
    }

    /*
     * Gets all reminders from database and populates the table.
     * Initiates the reminder timer task.
     */
    @PostConstruct
    private void init() {

        try {
            dataAccess.loadRemindersFromDb();
        }
        catch(DataAccessException ex) {

            dataAccessException = ex;
            return;
        }

        table.setItems(dataAccess.getAllReminders());
        list.getSelectionModel().selectFirst();
        table.requestFocus();
        table.getSelectionModel().selectFirst();
		
        initiateReminderTimer();
        actionStatus.setText("Welcome to Reminders!");
    }

    private void initiateReminderTimer() {
	
        timer = new Timer();
        long zeroDelay = 0L;
        long period = 60000L; // 60 * 1000 = 1 min

        // The timer runs once (first time) for the overdue reminders
        // and subsequently the scheduled task every (one) minute
        timer.schedule(remindersTask, zeroDelay, period);
    }

    /*
     * The following three get methods are referred from the AppStarter class.
     */
    public DataAccessException getAppDatabaseException() {
	
        return dataAccessException;
    }
	
    public Parent getView() {
	
        return hbPane;
    }
	
    public Timer getTimer() {
	
        return timer;
    }

    /*
     * Reminder group list's item selection change listener class.
     * On selecting a group the group's reminders are shown in the table.
     * For example, selecting Priority shows only the reminders with
     * priority == true.
     */
    private class ListSelectChangeListener implements ChangeListener<Number> {

        @Override
        public void changed(ObservableValue<? extends Number> ov,
                Number oldVal, Number newVal) {

            int ix = newVal.intValue();

            if (ix >= 0) {

                String groupStr = list.getItems().get(ix);	
                ReminderGroup group = ReminderGroup.getGroup(groupStr);
                actionStatus.setText("");
                table.setItems(dataAccess.getTableDataForGroup(group));
                table.requestFocus();
                table.getSelectionModel().selectFirst();
            }
        }
    }

    /*
     * Routine for new reminder button action.
     * The new reminder is edited in the ReminderDialog
     * and is inserted in the table and the database.
     */
    private void newReminderRoutine() {

        LocalDate reminderDate = localDatePicker.getLocalDate();
        reminderDate =
            (reminderDate == null || reminderDate.isBefore(LocalDate.now()))
                ? LocalDate.now() : reminderDate;
        Dialog<Reminder> dialog = reminderDialog.create(reminderDate);	
        Optional<Reminder> result = dialog.showAndWait();
		
        if (result.isPresent()) {

            try {
                dataAccess.addReminder(result.get());
            }
            catch(DataAccessException ex) {
		
                String msg = "A database error occurred while inserting the new reminder, exiting the app.";
                AppStarter.showExceptionAlertAndExitTheApp(ex, msg);
            }

            refreshTable();
            actionStatus.setText("New reminder added.");
            table.getSelectionModel().selectLast();
        }
        else {
            table.requestFocus();
        }
    }

    /*
     * Refreshes the table with the updated rows after a
     * reminder is added, updated or deleted.
     */
    private void refreshTable() {
	
        table.setItems(dataAccess.getAllReminders());
        list.getSelectionModel().selectFirst();
        table.requestFocus();
    }

    /*
     * Routine for update reminder button action.
     * The selected reminder in the table is edited in the
     * ReminderDialog and is updated in the table and the database.
     */
    public void updateReminderRoutine() {
	
        Reminder rem = table.getSelectionModel().getSelectedItem();
		
        if ((table.getItems().isEmpty()) || (rem == null)) {
		
            return;
        }

        int ix = dataAccess.getAllReminders().indexOf(rem);
        Dialog<Reminder> dialog = reminderDialog.create(rem);
        Optional<Reminder> result = dialog.showAndWait();
		
        if (result.isPresent()) {

            try {
                dataAccess.updateReminder(ix, result.get());
            }
            catch(DataAccessException ex) {
		
                String msg = "A database error occurred while updating the reminder, exiting the app.";
                AppStarter.showExceptionAlertAndExitTheApp(ex, msg);
            }

            refreshTable();
            actionStatus.setText("Reminder updated.");
            table.getSelectionModel().select(ix);
        }
        else {
            table.requestFocus();
        }
    }

    /*
     * Routine for delete reminder button action.
     * Deletes the selected reminder from table and the database.
     */
    private void deleteReminderRoutine() {

        Reminder rem = table.getSelectionModel().getSelectedItem();
			
        if (table.getItems().isEmpty() || rem == null) {
			
            return;
        }

        Alert confirmAlert = getConfirmAlertForDelete(rem);
        Optional<ButtonType> result = confirmAlert.showAndWait();
			
        if ((result.isPresent()) && (result.get() == ButtonType.OK)) {

            try{
                dataAccess.deleteReminder(rem);
            }
            catch(DataAccessException ex) {
		
                String msg = "A database error occurred while deleting the reminder, exiting the app.";
                AppStarter.showExceptionAlertAndExitTheApp(ex, msg);
            }
			
            refreshTable();
            actionStatus.setText("Reminder deleted.");	
            table.getSelectionModel().selectFirst();
        }
        else {
            table.requestFocus();
        }
    }
		
    private Alert getConfirmAlertForDelete(Reminder rem) {

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setTitle("Reminder");
        alert.setContentText("Delete reminder? " + rem);
        return alert;
    }
}