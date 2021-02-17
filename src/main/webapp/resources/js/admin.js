function handleMessageNotify(message) {
  if (message.lastIndexOf('MissingSaves:', 0) === 0) {
    document.getElementById("tabs:form1:missingSaves").innerHTML =
        message.substring(13);
  }
  else {
    console.log(message);
  }
}

function handleMessageTimer(message) {
  document.getElementById("tabs:form2:timer").innerHTML = message;
  document.getElementById("tabs:form2:delay").value = message;
  updateEvents();
}


