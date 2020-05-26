let date = new Date()

$( document ).ready(function() {
    $("#login")
        .mouseenter(function() {
            $( this ).attr("src", "btn_google_signin_dark_pressed_web.png");
        })
        .mouseleave(function() {
            $( this ).attr("src", "btn_google_signin_dark_normal_web.png");
        })
        .focusin(function() {
            $( this ).attr("src", "btn_google_signin_dark_focus_web.png");
        })
        .focusout(function() {
            $( this ).attr("src", "btn_google_signin_dark_normal_web.png");
        })
        .click(function () {
            let provider = new firebase.auth.GoogleAuthProvider();
            firebase.auth().signInWithPopup(provider);
        });
    $("#logout").click(function () {
        firebase.auth().signOut();
    });
    $("#prevLink").click(function () {
        date.setDate(date.getDate() - 1);
        changeDate();
    });
    $("#nextLink").click(function () {
        date.setDate(date.getDate() + 1);
        changeDate();
    });
    var firebaseConfig = {
        apiKey: "AIzaSyAykWP69-EnGlmcbnAJJ8Yy95O7mOcPig0",
        authDomain: "heartgardenreservation.firebaseapp.com",
        databaseURL: "https://heartgardenreservation.firebaseio.com",
        projectId: "heartgardenreservation",
        storageBucket: "heartgardenreservation.appspot.com",
        messagingSenderId: "522034061305",
        appId: "1:522034061305:web:d04cc8aa2431fef8a8a728"
    };
    firebase.initializeApp(firebaseConfig);
    firebase.auth().onAuthStateChanged(authStateObserver);
});

function changeDate() {
    $("#mainContent").hide();
    $("#spinner").show();
    $.get({
        url: "/admin/reservations",
        data: {
            fromDate: getIsoDate(),
            toDate: getIsoDate()
        },
        success: function (data) {
            $("#spinner").hide();
            $("#mainContent").show();
            $("#date").text(getIsoDate());
            let tbody = $("#tbody");
            tbody.empty();
            for (let i = 0; i < data.length; i++) {
                tbody.append($('<tr>')
                    .append($('<td>').text(data[i].date))
                    .append($('<td>').text(data[i].times.join(", ")))
                    .append($('<td>').text(data[i].name))
                    .append($('<td>').text(data[i].email))
                    .append($('<td>').text(data[i].nbOfGuests))
                    .append($('<td>').text(new Date(data[i].registered).toLocaleString('en-us')))
                    .append($('<td>')
                        .append($('<button>')
                            .addClass("btn")
                            .addClass("btn-danger")
                            .addClass("deleteButton")
                            .prop('type', 'button')
                            .data('customerUUID', data[i].customerUUID)
                            .data('customerName', data[i].name)
                            .text('Delete')
                        )
                    )
                );
            }
            $(".deleteButton").click(function () {
                let thisButton = $(this);
                if (window.confirm("Are you sure you want to delete the registration for " + thisButton.data("customerName") + "?")) {
                    $.ajax({
                        url: "/admin/reservation",
                        method: 'DELETE',
                        data: {
                            customerUUID: thisButton.data("customerUUID")
                        },
                        success: function () {
                            location.reload();
                        }
                    })
                }
            })
        }
    });
}

function getIsoDate() {
    return date.toISOString().substring(0, 10);
}

function authStateObserver(user) {
    if (user) { // User is signed in!
        $("#loginContainer").hide();
        $("#nickname").text(user.displayName);
        $("#avatar").prop('src', user.photoURL);
        $("#loggedInUserContainer").show();
        changeDate()
    } else { // User is signed out!
        $("#loginContainer").show();
        $("#loggedInUserContainer").hide();
        $("#mainContent").hide();
        $("#date").text("");
    }
}