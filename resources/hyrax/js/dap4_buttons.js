var enforce_selection;
function make_a_selection(){
    alert("Please select one or more variables before attempting to download/access data.");
}

/* Retired, Old button function
function getAs_button_action(type_name, suffix) {
    var url = new String(document.forms[0].url.value);

    var url_parts = url.split("?");
    // handle case where constraint is null.
    if (url_parts[1] != null && url_parts[1].length>0) {
        var get_as_url = url_parts[0] + suffix + "?" + url_parts[1];
    } else if(enforce_selection) {
        make_a_selection();
        return;
    }
    else {
        var get_as_url = url_parts[0] +  suffix + "?";
    }
    window.open(encodeURI(get_as_url),type_name);
}
*/

function response_encoding_change(){
    var encoding_suffix = String(document.forms[0].encoding.value);
    var encoding_name = String(document.forms[0].encoding[encoding.selectedIndex].text);
    alert("You picked: " + encoding_name + " (request suffix: " + encoding_suffix + ")");
}

/**
 * Based on Dean Taylor's StackOverflow answer:
 *     https://stackoverflow.com/questions/400212/how-do-i-copy-to-the-clipboard-in-javascript
 */
function copy_encoded_url_to_clipboard(){
    const debug_local = false;
    const encoded_request_url = encodeURI(document.forms[0].url.value)

    if (!navigator.clipboard) {
        if (debug_local || DEBUG.enabled()) {
            alert("Using fallback copy scheme document.execCommand('copy') to copy URI Encoded URL:\n"+encoded_request_url);
        }
        // Since the navigator.clipboard is not available we need to create a
        // temporary text area in the document to hold the URI encode URL value.
        // We'll set its value to the URI encoded URL, set the focus to the temporary
        // textarea, select it, copy it, and then remove the temporary textarea
        // from the document.

        var tmpTextArea = document.createElement("textarea");
        //tmpTextArea.hidden = true;
        tmpTextArea.value = encoded_request_url;

        // Avoid scrolling to bottom
        tmpTextArea.style.top = "0";
        tmpTextArea.style.left = "0";
        tmpTextArea.style.position = "fixed";

        // Add the textarea, set the focus to it, and select it.
        document.body.appendChild(tmpTextArea);
        tmpTextArea.focus();
        tmpTextArea.select();

        try {
            // Copy the value of the selected text area to the clipboard.
            var successful = document.execCommand('copy');
            var msg = successful ? 'successful' : 'unsuccessful';
            console.log('Fallback Copy: Copying text command was ' + msg);
        } catch (err) {
            console.error('Fallback Copy: Oops, unable to copy', err);
        }
        // Remove the temporary text area from the document
        alert("looky here");
        //document.body.removeChild(tmpTextArea);
        return;
    }

    if (debug_local || DEBUG.enabled()) {
        alert("Using navigator.clipboard.writeText() to copy URI Encoded URL:\n"+encoded_request_url);
    }

    navigator.clipboard.writeText(encoded_request_url).then(function() {
        console.log('Async: Copying to clipboard was successful!');
    }, function(err) {
        console.error('Async: Could not copy text: ', err);
    });
}

function getdata_button_action(){
    var url = String(document.forms[0].url.value);
    var type_name = String(document.forms[0].encoding[encoding.selectedIndex].text);
    var encoding_suffix = String(document.forms[0].encoding.value);

    if(encoding_suffix === ""){
        alert("OOPS! You must select a Download Encoding before you can get data!");
        return;
    }

    if(enforce_selection) {
        var url_parts = url.split("?");
        // If the constraint is empty then we ask the user to make a selection
        if (url_parts[1] == null || url_parts[1].length>0) {
            make_a_selection();
            return;
        }
    }
    window.open(encodeURI(url),type_name);
}


var help = 0;

// Our friend, the help window.

function help_button() {
    // Check the global to keep from opening the window again if it is
    // already visible. I think Netscape handles this but I know it will
    // write the contents over and over again. This preents that, too.
    // 10/8/99 jhrg
    if (help && !help.closed)
        return;

    // Resize on Netscape 4 is hosed. When enabled, if a user resizes then
    // the root window's document gets reloaded. This does not happen on IE5.
    // regardless, with scrollbars we don't absolutely need to be able to
    // resize. 10/8/99 jhrg
    help = window.open("http://www.opendap.org/online_help_files/opendap_form_help.html",
        "help", "scrollbars,dependent,width=600,height=400");
}

//function open_dods_home() {
//    window.open("http://www.opendap.org/", "DAP_HOME_PAGE");
//}


// Helper functions for the form.

function describe_index() {
    window.status = "Enter start, stride and stop for the array dimension.";
    return true;
}

function describe_selection() {
    window.status = "Enter a relational expression (e.g., <20). String variables may need values to be quoted";
    return true;
}

function describe_operator() {
    window.status = "Choose a relational operator. Use - to enter a function name).";
    return true;
}

function describe_projection() {
    window.status = "Add this variable to the projection.";
    return true;
}


/* Auto resize a text input field */
function autoResize(e) {
    var ele = e.target; //get the input field

    if (DEBUG.enabled()) alert("autoresize: \n" + "\nsize: " + ele.size + "\nvalue: " + ele.value + "\nvalue.length: " + ele.value.length);

    if (ele.size <= ele.value.length) {
        ele.size = ele.value.length * 1.4;
    }


}


/**************************************************************
 *
 * The DEBUG object
 *
 */
function debug_obj() {
    this.enabled = function () {
        if(this.myCheckBox)
            return this.myCheckBox.checked;
        else
            return false;
    }
    this.setCheckBox = function(checkBox){
        this.myCheckBox = checkBox;
    }
}


/***********************************************************************
 *
 * The dap4_dataset object.
 *
 *
 */
function dap4_dataset(base_url) {
    this.url = base_url;
    this.constraintExpression = "";
    this.num_dap_vars = 0;
    this.dap_vars = new Array();

    /*
     * Add the current projection and selection to the URL
     */
    this.update_url = function () {

        var msg = "Updated Data Request URL.\nold url: "+this.url+"\n";
        var encoding_suffix = String(document.forms[0].encoding.value);

        this.build_DAP4_constraint();
        var url_text = this.url + encoding_suffix;
        // Only add the projection & selection (and ?) if there really are
        // constraints!
        if (this.constraintExpression.length > 0) {
            url_text += "?dap4.ce=" + this.constraintExpression;
        }
        document.forms[0].url.value = url_text;
        if (DEBUG.enabled()) {
            msg = msg + "new url: "+this.url;
            alert(msg);
        }
    }


    /*
     * Scan all the form elements and pick out the various pieces of constraint
     * information. Set the dap_url state to reflect the new information.
     */
    this.build_DAP4_constraint = function () {
        var ce = "";
        for (var i = 0; i < this.num_dap_vars; ++i) {
            var dapVar = this.dap_vars[i];
            var varProj = dapVar.getDap4CE();
            if (varProj.length > 0) {
                if (ce.length > 0)
                    ce += ";";
                ce += varProj;
            }
        }
        this.constraintExpression = ce;
    }

    /*
     * Add the variable to the array of dap_vars. The var_index is the
     * number of *this particular* variable in the dataset, zero-based.
     */
    this.add_dap_var = function (dap_var) {
        this.dap_vars[this.num_dap_vars] = dap_var;
        this.num_dap_vars++;
    }

}

/***********************************************************************/


/***********************************************************************
 * SelectionClause
 *
 * selectionOperator: the operator for this selection clause (<, <=, ==, !=, >=, >)
 * selectionValue: the right side value for the clause.
 */
function SelectionClause(selectionId, relOpWidget, rValueWidget) {
    this.selectionId = selectionId;
    this.relOpWidget = relOpWidget;
    this.rValueWidget = rValueWidget;

    this.relOp = function () {
        return relOpWidget[relOpWidget.selectedIndex].value;
    }

    this.rValue = function () {
        return rValueWidget.value;
    }

}

/***********************************************************************/


/***********************************************************************
 *
 * dap_var
 *
 * name: the name of the variable from DAP perspective.
 * js_var_name: the name of the variable within the form.
 * isArray: true if this is an array, false otherwise.
 * isContainer: true if this is a container type, false otherwise.
 */
function dap_var(name, js_var_name, isArray, isContainer) {
    // Common members
    this.name = name;
    this.js_var_name = js_var_name;
    this.isArray = isArray;
    this.isContainer = isContainer;

    this.projected = false;

    this.numSelectionClauses = 0;        // Holds the number of child variables
    this.selectionClauses = new Array(); // Holds the chid variables


    /* DAP ARRAY TYPE -------------------------------------------------------------------
     * If this is DAP Array type then add the array
     * manipulation machinery to this instance
     *
     */
    if (isArray) {
        if (DEBUG.enabled()) alert(js_var_name + " is an array");
        this.num_dims = 0;       // Holds the number of dimensions
        this.dims = new Array(); // Holds the length of the dimensions
        this.dimTextBoxes = new Array(); // Holds the names of the dimensions textbox fields


        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *
         * dap_var.addDimension()
         *
         * Add a dimension to a DAP Array object.
         *
         * dimTextBox - The UI element which specifies the dimesions
         *              projection/hyperslab
         * size - The size of the dimension.
         *
         */
        this.addDimension = function (dimTextBox, size) {
            if (DEBUG.enabled()) alert(this.js_var_name + " adding dimension " + this.num_dims + " size: " + size);
            this.dimTextBoxes[this.num_dims] = dimTextBox;
            this.dims[this.num_dims] = size;
            this.num_dims++;
        };
        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *
         * dap_var.display_indices()
         *
         * Add the array indices to the UI text widgets associated with
         * this DAP array object. The text widgets are names
         * <var_name>_0, <var_name>_1, ... <var_name>_n for an array
         * with size N+1.
         *
         */
        this.display_indices = function () {
            if (DEBUG.enabled()) alert(this.name + " display_indices()\nnum_dims: " + this.num_dims);
            for (var i = 0; i < this.num_dims; ++i) {
                var tBox = this.dimTextBoxes[i];
                // Check the text box - if it already has content don't overwrite it
                if (tBox.value.length == 0) {
                    var end_index = this.dims[i] - 1;
                    var s = "0:1:" + end_index.toString();
                    tBox.value = s;
                }
            }
        };
        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *
         * dap_var.erase_indices()
         *
         * Clears hyperslab index information from a DAP array
         * object UI.
         *
         */
        this.erase_indices = function () {
            for (var i = 0; i < this.num_dims; ++i) {
                var tboxName = this.dimTextBoxes[i];
                tboxName.value = "";
            }
        };
        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *
         * dap_var.isDefaultArrayProjection()
         *
         * Returns true if the default array projection (hyperslab)
         * is specified for this array variable.
         *
         */
        this.isDefaultArrayProjection = function () {
            for (var i = 0; i < this.num_dims; ++i) {
                var tBox = this.dimTextBoxes[i];
                var end_index = this.dims[i] - 1;
                var defaultProj = "0:1:" + end_index.toString();
                if (tBox.value != defaultProj) {
                    return false;
                }
            }
            return true;
        };
        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

    }
    /* END ARRAY TYPE ---------------------------------------------------------------*/


    /* CONTAINER TYPE -------------------------------------------------------------
     * If this is a container type then add the container
     * manipulation machinery to this instance
     */
    if (this.isContainer) {
        this.numChildren = 0;        // Holds the number of child variables
        this.children = new Array(); // Holds the chid variables

        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *
         * dap_var.addChildVar()
         *
         * Adds a child variable to the container.
         *
         */
        this.addChildVar = function (childVar) {
            this.children[this.numChildren] = childVar;
            this.numChildren++;
        };
        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *
         * dap_var.descendantArraysAreCustomHyperSlabbed()
         *
         * Returns true if there is a descendant array of this container
         * that is projected with a custom (non-default) hyperslab
         *
         */
        this.descendantArraysAreCustomHyperSlabbed = function () {
            for (var i = 0; i < this.numChildren; i++) {
                var childVar = this.children[i];
                if (childVar.isArray && !childVar.isDefaultArrayProjection()) {
                    return true;
                }

                if (childVar.isContainer) {
                    return childVar.descendantArraysAreCustomHyperSlabbed();
                }

            }
            return false;

        };
        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *
         * dap_var.getDescendantsProjection()
         *
         * Returns the cumulative projection string of this containers
         * descendants.
         *
         */
        this.getDescendantsProjection = function () {
            var p = "";
            for (var i = 0; i < this.numChildren; i++) {
                var descendantsProjection = this.children[i].get_projection();
                if (descendantsProjection != "") {
                    if (p != "") {
                        p += ";";
                    }
                    p += descendantsProjection;
                }
            }
            return p;
        };
        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *
         * dap_var.hasProjectedChildren()
         *
         * Returns true if this variable is a container that has
         * projected children, false otherwise.
         *
         */
        this.hasProjectedChildren = function () {

            var foundProjectedChild = false;
            for (var i = 0; i < this.numChildren && !foundProjectedChild; i++) {

                if (this.children[i].isContainer) {
                    foundProjectedChild = this.children[i].hasProjectedChildren();
                }
                else {
                    foundProjectedChild = this.children[i].isProjected();
                }
            }


            return foundProjectedChild;
        };
        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


    }
    /* END CONTAINER TYPE -----------------------------------------------------*/


    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * dap_var.handle_projection_change()
     *
     * This method handles a change in state trigger by the UI
     * dectating a state change of the projection checkBox
     * associated with this variable.
     *
     *
     */
    this.handle_projection_change = function (check_box) {

        if (DEBUG.enabled()) alert(this.name + " Handling projection change.\n" +
            "isContainer(): " + this.isContainer + "\n" +
            "isArray(): " + this.isArray + "\n");

        this.setProjected(check_box.checked);

        this.updateProjection();
        if (DEBUG.enabled()) showProjection();
        this.updateChecked();
        DAP4_DATASET.update_url();
    };

    /*
     *
     */
    this.isProjected = function () {
        return this.projected;
    };
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * dap_var.setProjected()
     *
     * Sets the projection state for this variable. If this
     * variable is a container then all of its children state
     * are also set. If it is an array then the default
     * hyper slab state is set for each dimension.
     *
     */
    this.setProjected = function (isProj) {

        if (DEBUG.enabled()) alert("setting proj on " + this.name + " to " + isProj);
        this.projected = isProj;

        if (this.isArray) {
            if (isProj) {
                if (DEBUG.enabled()) alert(this.name + " displaying indices...");
                this.display_indices();
            }
            else {
                if (DEBUG.enabled()) alert(this.name + " hiding indices...");
                this.erase_indices();
            }
        }

        if (this.isContainer) {
            if (DEBUG.enabled()) alert("setting proj on children of container " + this.name);
            for (var i = 0; i < this.numChildren; i++) {
                var childVar = this.children[i];
                childVar.setProjected(isProj);
            }
        }
    };
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * dap_var.updateProjection()
     *
     * Updates the projection for the children of containers -
     * when all the children of a container are projected then the
     * parent conationer is projected.
     */
    this.updateProjection = function () {

        if (this.parentContainer != null) {
            this.parentContainer.updateProjection();
        }
        else {
            if (DEBUG.enabled()) alert("Updating proj starting at " + this.name);
            this.updateProjWorker();
        }
    };
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * dap_var.updateProjWorker()
     *
     * Recursive worker method for dap_var.updateProjection()
     *
     */
    this.updateProjWorker = function () {

        if (this.isContainer) {
            var currentProj = this.projected;

            var allChildrenProjected = true;
            var noChildrenProjected = true;
            for (var i = 0; i < this.numChildren; i++) {
                var childVar = this.children[i];
                if (childVar.isContainer)
                    childVar.updateProjWorker();
                allChildrenProjected = allChildrenProjected && childVar.projected;
                noChildrenProjected = noChildrenProjected && !childVar.projected;
            }
            if (allChildrenProjected) {
                if (DEBUG.enabled()) alert(this.name + "  allChildrenProjected: " + allChildrenProjected);
                this.setProjected(true);
            }
            else if (noChildrenProjected) {
                if (DEBUG.enabled()) alert(this.name + "  noChildrenProjected:" + noChildrenProjected);
                this.projected = false;
            }
            else {
                this.projected = false;
            }
        }
    };
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * dap_var.getArrayCE()
     * 
     * Returns the constraint expression clause associated with this 
     * and (if it's a container) its children.
     * 
     */
    this.getDap4CE = function () {

        ce = "";

        var projection = this.get_projection();

        if (projection.length > 0) {
            ce += projection;
        }

        var filterExpression = this.get_selection();

        if (filterExpression.length > 0) {
            ce += "|" + filterExpression;
        }

        return ce;


    }
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * dap_var.get_projection()
     *
     * Get the projection sub-expression for this variable.
     *
     */
    this.get_projection = function () {
        var p = "";

        if (this.isProjected()) {
            if (this.isArray) {
                p = this.name;
                // ***
                for (var i = 0; i < this.num_dims; ++i) {
                    p += "[" + this.dimTextBoxes[i].value + "]";
                }
            } else {

                if (this.isContainer) {

                    if (this.descendantArraysAreCustomHyperSlabbed()) {
                        p = this.getDescendantsProjection();
                    }
                    else {
                        p = this.name;
                    }
                }
                else {
                    p = this.name;
                }
            }

        }
        else if (this.isContainer && this.hasProjectedChildren()) {
            p = this.getDescendantsProjection();
        }

        return p;
    };
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * dap_var.addSelectionClause()
     *
     * Adds a selection clause for the variable
     *
     */
    this.addSelectionClause = function (selectionId, relOpWidget, rValueWidget) {
        var sClause = new SelectionClause(selectionId, relOpWidget, rValueWidget);
        this.selectionClauses[this.numSelectionClauses] = sClause;
        this.numSelectionClauses++;
    };
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * dap_var.get_selection()
     *
     * Get the selection clauses for this variable.
     *
     */
    this.get_selection = function () {
        var s = "";

        if (this.isContainer) {
            for (var i = 0; i < this.numChildren; i++) {
                childSelections = this.children[i].get_selection();
                if (childSelections.length > 0) {
                    if (s.length > 0) {
                        s += "&";
                    }
                    s += childSelections;
                }
            }
        }
        else {
            for (var i = 0; i < this.numSelectionClauses; i++) {
                var selection = document.getElementById(this.selectionClauses[i].selectionId);
                var relOp = this.selectionClauses[i].relOp();
                var rValue = this.selectionClauses[i].rValue();

                if (rValue.length > 0) {
                    if (s.length > 0)
                        s += "&";
                    s = this.name + relOp + rValue;

                    selection.style.backgroundColor = "#90BBFF";
                    selection.style.fontStyle = "bold";
                    selection.style.border = "solid";
                    selection.style.borderWidth = "1px";

                    if (DEBUG.enabled()) alert(this.name + "Selection Clause:\n" + s);
                }
                else {
                    selection.style.backgroundColor = "white";
                    selection.style.border = "none";


                }
            }
        }
        return s;
    };
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * dap_var.updateChecked()
     *
     * Updates the check boxes in the UI to reflect the current
     * projection
     *
     */
    this.updateChecked = function () {

        if (this.parentContainer != null) {
            this.parentContainer.updateChecked();
        }
        else {
            if (DEBUG.enabled()) alert("Updating checkbox states starting at " + this.name);
            this.updatedCheckedWorker();
        }
    }
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/


    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * dap_var.updatedCheckedWorker()
     *
     * A recursive helper method for updateChecked()
     *
     */
    this.updatedCheckedWorker = function () {

        var myCheckBox = document.getElementById(this.checkBox);

        if (this.isProjected()) {
            myCheckBox.checked = true;
            if (this.isContainer) {
                myCheckBox.indeterminate = false;
            }
        }
        else {
            myCheckBox.checked = false;
            if (this.isContainer) {
                myCheckBox.indeterminate = this.hasProjectedChildren();
            }
        }

        if (this.isContainer) {
            for (var i = 0; i < this.numChildren; i++) {
                this.children[i].updatedCheckedWorker();
            }
        }

    }
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

}

/***********************************************************************/


function showProjection() {

    var msg = "Projection Report:\n";
    for (var i = 0; i < DAP4_DATASET.num_dap_vars; i++) {
        var dapVar = DAP4_DATASET.dap_vars[i];
        msg += dapVar.name + ": " + dapVar.projected + "\n";
        if (dapVar.hasProjectedChildren()) {
            msg += "Projected Children: \n" + dapVar.get_projection();
        }
    }
    alert(msg);
}



