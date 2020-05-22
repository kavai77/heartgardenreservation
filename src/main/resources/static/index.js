$( document ).ready(function() {
    fillDates();
    fillTimes();
    fillNbOfGuest();
    setupFormUpdate();
});

var elementsToDisableIfNoSlots = ["#timeInput", "#submit", "#nbOfGuest"]

function fillDates() {
    let dateInput = $('#dateInput');
    for (i = 0; i < 8; i++) {
        var date = new Date();
        date.setDate(date.getDate() + i);
        if (date.getDay() === 1) continue;
        var value
        if (i === 0) {
            value = "Today";
        } else if (i === 1) {
            value = "Tomorrow"
        } else {
            value = date.toLocaleString('en-us', { month: 'long', weekday: 'long', day: 'numeric' });
        }
        var key = date.toISOString().substring(0, 10);
        dateInput.append($("<option></option>").attr("value", key).text( value));
    }

    dateInput.on('change', fillTimes)
}

function fillTimes() {
    let timeInput = $('#timeInput');
    timeInput.empty()
    var selectedDate = $("#dateInput").prop('selectedIndex')
    var date = new Date();
    for (i = 0; i < 14; i++) {
        var hour = 9 + Math.floor(i / 2)
        var minute = (i % 2) === 0 ? "00" : "30"
        var time = hour + ":" + minute
        if (selectedDate === 0 &&
            (hour < date.getHours() ||
            (date.getHours() === hour && (i % 2) === 0 && date.getMinutes() > 0) ||
            (date.getHours() === hour && (i % 2) === 1 && date.getMinutes() > 30))) {
            continue;
        }
        timeInput.append($("<option></option>").attr("value", time).text(time));
    }
    var isOptionEmpty = $('#timeInput option').length === 0
    if (isOptionEmpty) {
        timeInput.append($("<option></option>").text("No available times today"));
    }
    elementsToDisableIfNoSlots.forEach(function(item) {
        $(item).prop('disabled', isOptionEmpty);
    });
}

function fillNbOfGuest() {
    for (i = 1; i <= 4; i++) {
        $('#nbOfGuest').append($("<option></option>").attr("value", i).text(i));
    }
}

function setupFormUpdate() {
    $("#reservationForm").submit(function(e) {
        $(this).addClass('was-validated');
        return $(this)[0].checkValidity()
    });
}