import {Info_Town} from "/script/Info_Town.js";

// 下拉式選單依據選單變化標題
$(document).ready(function () {
  $(".dropdown-menu a").click(function (e) {
    e.preventDefault();
    $(this).parents(".dropdown").find('.btn').html('<span id='+$(this).parents(".dropdown").find('.btn').find("span").attr('id')+'>'+$(this).text()+'</span>');
    $(this).parents(".dropdown").find('.btn').val($(this).data('value'));
  });
});

// 時間選擇器
$(document).ready(function () {
  $('timepicker').timepicker({
    timeFormat: 'HH:mm',
    interval: 15,
    minTime: '00:00',
    maxTime: '23:45',
  });
});

// function readyToChange() {
//   $(".dropdown-menu a").click(function (e) {
//     e.preventDefault();
//     $(this).parents(".dropdown").find('.btn').html($(this).text() + ' <span class="caret"></span>');
//     $(this).parents(".dropdown").find('.btn').val($(this).data('value'));
//   });
// }

//下拉式選單選項因選項變更
$(document).ready(function () {
  $('#dropdown-menu-1 .dropdown-item').click(function () {
    let selectedValue = $(this).data("value");

    let newOptions = [];
    
    for(let x = 0; x<Info_Town[selectedValue].length; x++){
      let text = Info_Town[selectedValue][x]["Name"]["C"];
      let id = Info_Town[selectedValue][x]["ID"];
      
      newOptions.push({ value: id, text: text });

    }

    // 清空選項
    $('#dropdown-menu-2').empty();

    // 預設第一選項
    $('#dropdownMenuButton-2').html('<span id="town">'+ newOptions[0].text +'</span>');

    // 取消禁用按鈕
    $('#dropdownMenuButton-2').removeClass('disabled');
    $('#changeSetting').removeClass('disabled');

    // 将新的选项添加到第二个下拉式选单中
    $.each(newOptions, function (index, option) {
      $('#dropdown-menu-2').append('<a class="dropdown-item" href="#" data-value="' + option.value + '">' + option.text + '</a>');
    });

  })
});

$(document).ready(function () {
  $('#dropdown-menu-3 .dropdown-item').click(function () {
    $('#changeSetting').removeClass('disabled');
  })
});