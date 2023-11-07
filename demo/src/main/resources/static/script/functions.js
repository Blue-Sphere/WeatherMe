async function changeSetting() {
    let newData = {
      obsId: document.getElementById("obsId").value,
      city: document.getElementById("city").textContent,
      town: document.getElementById("town").textContent,
      notificationTime: document.getElementById("notificationTime").textContent
    };
    await fetch("/changeSetting", {method: "POST",
                                  headers: {"Content-Type": "application/json","Authorization": `Bearer ${getCookie("token")}`},
                                  body: JSON.stringify(newData)})
    .then(response => response.text())
    .then(data => {
        alert(data);
        location.reload();
    })
    .catch(error => alert(error));
  }
  
function logout(){
  try{
    document.cookie = 'token' + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    alert("登出成功");
    window.location.href = "login.html";
  }catch{
    alert("登出失敗");
  }
}

function lineLogin(){
  let url = "https://access.line.me/oauth2/v2.1/authorize?";
  let response_type = "response_type=code";
  let client_id = "client_id=2000634910";
  let redirect_uri = "redirect_uri=https://weatherme-3vsl.onrender.com/line/connect";
  let state = "state=login";
  let scope = "scope=openid";
  let loginUrl = url + response_type + "&" + client_id + "&" + redirect_uri + "&" + state + "&" + scope;
  window.location.href = loginUrl;
}