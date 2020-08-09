$( document ).ready(function() {
    let config;
    let slots;
    $.when(
        $.get({
            url: "/config",
            success: function (data) {
                config = data;
            }
        }),

        $.get({
            url: "/slots",
            success: function (data) {
                slots = data;
            }
        })

    ).then(function() {
        const reservation = new Reservation(config, slots)
        reservation.fillNbOfGuest();
        reservation.fillDates();
        reservation.fillTimes();
    });

    $('#spinnerModal').modal({backdrop: 'static', keyboard: false, show: false});
});

class Reservation {
    constructor(config, slots) {
        this.config = config;
        this.slots = slots;
    }

    fillDates() {
        let searchParams = new URLSearchParams(window.location.search)
        let lang = searchParams.get('lang')
        if (lang === null) {
            lang = "en";
        }
        let dateInput = $('#dateInput');
        const now = new Date();
        const today = now.toISOString().substring(0, 10);
        now.setDate(now.getDate() + 1);
        const tomorrow = now.toISOString().substring(0, 10);
        for (let i = 0; i < this.slots.length; i++) {
            let value
            if (this.slots[i].date === today) {
                value = $("#today").text();
            } else if (this.slots[i].date === tomorrow) {
                value = $("#tomorrow").text();
            } else {
                const date = new Date(this.slots[i].date);
                value = date.toLocaleString(lang, { month: 'long', weekday: 'long', day: 'numeric' });
            }
            dateInput.append($("<option></option>").attr("value", this.slots[i].date).text(value));
        }
        const self = this;
        dateInput.change(function () { self.fillTimes() });
    }

    fillTimes() {
        const timeInput = $('#timeInput');
        timeInput.empty();
        const selectedSlot = this.findSelectedDate();
        const necessarySpaces = parseInt($('#nbOfGuests').find("option:selected").data("tables"));
        for (let i = 0; i < selectedSlot.slotTimes.length; i++) {
            const slot = selectedSlot.slotTimes[i];
            const enabled = !slot.disabled &&
                this.slotEnabled(selectedSlot.slotTimes, i, this.config.slotsPerReservation, necessarySpaces);
            timeInput.append($("<option></option>").attr("value", slot.time).attr("disabled", !enabled).text(slot.text));
        }
        const isOptionEmpty = selectedSlot.slotTimes.length === 0
        if (isOptionEmpty) {
            timeInput.append($("<option></option>").text($("#noavailabletimetoday").text()));
        }
        const elementsToDisableIfNoSlots = ["#timeInput", "#submitButton", "#nbOfGuest"];
        elementsToDisableIfNoSlots.forEach(function(item) {
            $(item).prop('disabled', isOptionEmpty);
        });
    }

    slotEnabled(slotTimes, startIndex, slotsPerReservation, necessarySpaces) {
        const endIndex = Math.min(startIndex + slotsPerReservation, slotTimes.length);
        for (let i = startIndex; i < endIndex; i++) {
            if (slotTimes[i].freeTables < necessarySpaces) {
                return false;
            }
        }
        return true;
    }

    findSelectedDate() {
        const selectedDate = $("#dateInput").val();
        for (let i = 0; i < this.slots.length; i++) {
            if (this.slots[i].date === selectedDate) {
                return this.slots[i];
            }
        }
    }

    fillNbOfGuest() {
        const guestNb = []
        for (let [key, value] of Object.entries(this.config.guestTableNbMap)) {
            guestNb.push(parseInt(key));
        }
        guestNb.sort();
        const nbOfGuests = $('#nbOfGuests');
        for (let i = 0; i < guestNb.length; i++) {
            const guest = guestNb[i].toString();
            nbOfGuests.append($("<option></option>")
                .attr("value", guest)
                .data("tables", this.config.guestTableNbMap[guest])
                .text(guest));
        }
        const self = this;
        nbOfGuests.change(function () {
            const guests = parseInt(nbOfGuests.val());
            if (guests > self.config.oneHouseHoldLimitInForm) {
                $('#bigGroupInfo').show();
            } else {
                $('#bigGroupInfo').hide();
            }
            let timeInput = $('#timeInput');
            const selectedTime = timeInput.val();
            self.fillTimes();
            timeInput.val(selectedTime);
        })
    }
}

function onSubmit() {
    let form = $("#reservationForm");
    form.addClass('was-validated');
    if (form[0].checkValidity()) {
        $('#spinnerModal').modal('show');
        form.submit();
    }
}