function isNameValid(namestring) {
    if (namestring != null && namestring != "") {
        for (var j = 0; j < namestring.length; j++)
        {
            var ch = namestring.charAt(j);
            var code = ch.charCodeAt(0);
            if ((code > 47 && code < 59) // number
                    || (code > 64 && code < 91)// capital 
                    || (code > 96 && code < 123)// simple
                    || (code == 46) //dot
                    || (code == 95) // underscore
                    || (code == 2013) // en dash
                    || (code == 2014) // em dash
                    || (code == 45)) // minus sign - hyphen
            {
            }
            else {
                return false;
            }
        }
        return true;
    } else {
        return false;
    }
}

function isNumber(numberstring) {
    if (numberstring != null && numberstring != "") {
        for (var j = 0; j < numberstring.length; j++) {
            var ch = numberstring.charAt(j);
            var code = ch.charCodeAt(0);
            if (code > 47 && code < 59) {
            } else {
                return false;
            }
        }
        return true;
    } else {
        return false;
    }
}

function forward(destinationJSP) {
    location.href = destinationJSP;
}

function validateTaskInputs() {
    var taskName = document.getElementById('taskName').value;
    var triggerInterval = document.getElementById('triggerInterval').value;
    var triggerCount = document.getElementById('triggerCount').value;
    var serviceList = document.getElementById('serviceList');
    var serviceName = serviceList[serviceList.selectedIndex].value;
    var operationName = document.getElementById('operationName').value;
    var scheduleType = document.getElementById('scheduleType').value;
    var taskClass = document.getElementById('dssTaskClass').value;
    if (taskName == "" || taskName == null) {
        CARBON.showWarningDialog("任务名称不能为空");
        return false;
    }
    if (triggerCount == "" || triggerCount == null) {
        CARBON.showWarningDialog("任务重复计数不能为空");
        return false;
    }
    if (!(parseInt(triggerCount) >= -1)) {
    	CARBON.showWarningDialog("任务重复计数必须大于等于-1");
        return false;
    }
    if (triggerInterval == "" || triggerInterval == null) {
        CARBON.showWarningDialog("任务间隔不能为空");
        return false;
    }
    // triggerCount == 0 and triggerInterval == 0 is valid, 
    // since there need not be a trigger interval for an one time trigger.
    if (!(parseInt(triggerInterval) > 0) && !(parseInt(triggerCount) == 0 && parseInt(triggerInterval) == 0)) {
    	CARBON.showWarningDialog("任务间隔必须大于0");
        return false;
    }
    if (scheduleType == "" || scheduleType == null) {
        CARBON.showWarningDialog("选择计划类型");
        return false;
    } else if (scheduleType == "DataService Operation") {
        if (serviceName == "" || serviceName == null) {
            CARBON.showWarningDialog("选择有效的数据服务名称");
            return false;
        }
        if (operationName == "" || operationName == null) {
            CARBON.showWarningDialog("选择有效的数据服务操作");
            return false;
        }
    } else if (scheduleType == "DataService Task Class") {
        if (taskClass == "" || taskClass == null) {
            CARBON.showWarningDialog("任务类不能为空");
            return false;
        }
    }
    if (!isNameValid(taskName)) {
        CARBON.showWarningDialog(namemsg);
        return false;
    }
    var trigger = document.getElementById("taskTrigger");
    var triggerValue = getCheckedValue(trigger);
    if (triggerValue == 'simple') {
        if (triggerInterval != undefined && triggerInterval != null) {
            if (!isNumber(trim(triggerInterval))) {
                CARBON.showWarningDialog("请输入有效的任务间隔值");
                return false;
            }
        }
    }
    return true;
}

function settrigger(type) {
    var triggerCountTR = document.getElementById("triggerCountTR");
    var triggerIntervalTR = document.getElementById("triggerIntervalTR");
    var triggerCronTR = document.getElementById("triggerCronTR");
    var taskTrigger_hidden = document.getElementById("taskTrigger_hidden");
    taskTrigger_hidden.value = type;

    triggerIntervalTR.style.display = "";
    triggerCountTR.style.display = "";
    triggerCronTR.style.display = "none";

    return true;
}

function getCheckedValue(radioObj) {
    return "simple";
}

function onpropertyTypechange(index) {
    var indexstr = index.toString();
    var propertyType = document.getElementById(("propertyTypeSelection" + indexstr).toString());
    var propertyType_indexstr = null;
    var propertyType_value = null;
    if (propertyType != null)
    {
        propertyType_indexstr = propertyType.selectedIndex;
        if (propertyType_indexstr != null) {
            propertyType_value = propertyType.options[propertyType_indexstr].value;
        }
    }
    var textField = document.getElementById("textField" + index);
    var textArea = document.getElementById("textArea" + index);
    if (propertyType_value != null && propertyType_value != undefined && propertyType_value != "") {
        if (propertyType_value == 'literal') {
            textField.style.display = "";
            textArea.style.display = "none";

        } else if (propertyType_value == 'xml') {
            textField.style.display = "none";
            textArea.style.display = "";
        }
    }
}


function deleteproperty(i) {
    var propRow = document.getElementById("pr" + i);
    if (propRow != undefined && propRow != null) {
        var parentTBody = propRow.parentNode;
        if (parentTBody != undefined && parentTBody != null) {
            parentTBody.removeChild(propRow);
            if (!isContainRaw(parentTBody)) {
                var propertyTable = document.getElementById("property_table");
                propertyTable.style.display = "none";
            }
        }
    }
}

function isContainRaw(tbody) {
    if (tbody.childNodes == null || tbody.childNodes.length == 0) {
        return false;
    } else {
        for (var i = 0; i < tbody.childNodes.length; i++) {
            var child = tbody.childNodes[i];
            if (child != undefined && child != null) {
                if (child.nodeName == "tr" || child.nodeName == "TR") {
                    return true;
                }
            }
        }
    }
    return false;
}

function deleteRow(name, msg){
	CARBON.showConfirmationDialog(msg + "' " + name + " ' ?", function() {
        document.location.href = "saveTask.jsp?" + "taskName=" + name + "&saveMode=delete";
    });
}

function editRow(name) {
    document.location.href = "editTask.jsp?" + "taskName=" + name + "&saveMode=edit";
}

function onclassnamefieldchange(id) {
    var classname = document.getElementById("taskClass").value;
    if (classname != null && classname != undefined && classname != "") {
        document.getElementById(id).style.display = "";
    } else {
        document.getElementById(id).style.display = "none";
    }
}

function ltrim(str) {
    for (var k = 0; k < str.length && str.charAt(k) <= " "; k++) ;
    return str.substring(k, str.length);
}
function rtrim(str) {
    for (var j = str.length - 1; j >= 0 && str.charAt(j) <= " "; j--) ;
    return str.substring(0, j + 1);
}

function trim(stringValue) {
    return ltrim(rtrim(stringValue));
}

function goBackOnePage() {
    history.go(-1);
}

function getOperations(obj) {
    var serviceList = document.getElementById("serviceList");
    var selectedServiceId = serviceList[serviceList.selectedIndex].value;
    document.getElementById('dataServiceName').value = selectedServiceId;
    if (selectedServiceId != "") {
        var serviceUrl = "serviceId_ajaxprocessor.jsp";
        $.ajax({
            url:serviceUrl,
            global:false,
            type:"POST",
            data:({serviceId:selectedServiceId}),
            dataType:"html",
            async:false,
            success:function(data) {
                //added to skip processing copyright notice
                var copyrightSkippedData = data.replace(/<!--[\s\S]*?-->/g,'');
                var operations = copyrightSkippedData.split(",");
                var operationList = $('#operationList');
                $('>option', operationList).remove();
                operationList.append('<option value="">------------------------SELECT----------------------------</option>');
                $.each(operations, function(key, value) {
                    //operationList.append($('<option/>').val(key).text(value));
                    operationList.append($('<option id='+ key +' value='+ value +'>'+ value +'</option>'))
                });
            }
        });
    } else {
        var operationList = $('#operationList');
        if (operationList != null) {
            $('>option', operationList).remove();
        }
    }
}

function setOperationName() {
    var value = document.getElementById('operationList')[document.getElementById('operationList').selectedIndex].value;
    document.getElementById('operationName').value = value;
}

function getScheduleType(val) {
    if (val == "DataService Operation") {
        document.getElementById('dsTaskService').style.display = "";
        document.getElementById('dsTaskOperation').style.display = "";
        document.getElementById('dssTaskClassRow').style.display = "none";
    } else if (val == "DataService Task Class") {
        document.getElementById('dsTaskService').style.display = "none";
        document.getElementById('dsTaskOperation').style.display = "none";
        document.getElementById('dssTaskClassRow').style.display = "";
    }
}
