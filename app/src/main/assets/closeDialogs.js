document.querySelectorAll('[close]').forEach(el => {
  if (el.innerText == "CLOSE" || el.innerText == "IGNORE" || el.innerText == "CANCEL"){
    el.click();
  }
});
