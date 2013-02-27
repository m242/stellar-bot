$ ->

  $("#container-home").each ->

    $signin = $("#signin")
    $signin.find(".icon").removeClass("fadeinout-enabled")
    $signin.on "click", -> $signin.find(".icon").addClass("fadeinout-enabled")

  $("#container-loggedin").each ->

    $this = $(this)
    $this.find(".icon").removeClass("fadeinout-enabled")
    $this.find("#deleteBtn").each -> $("#robotImg").addClass("robotmove-enabled")

    $this.on "click", "#deleteBtn", ->
      $(this).find(".icon").addClass("fadeinout-enabled")
      # Ajax delete
      future = $.ajax({ url: "/stellar", type: "DELETE" })
      future.done (data) ->
        $("#robotImg").removeClass("robotmove-enabled")
        # Success message
        $("#deleteBtn").hide()
        $("#deleteSuccess").fadeIn(400).delay(4000).fadeOut(400, ->
          # Activate form
          html = "<form id=\"stellarForm\" method=\"POST\" action=\"/stellar\"><input id=\"stellarNameInput\" type=\"text\"" +
          " name=\"stellarName\" value=\"\" class=\"input-block-level\" placeholder=\"Example: mark242\" autocomplete=\"off\">" +
          " <button id=\"addBtn\" class=\"btn btn-info btn-block btn-large\"><em class=\"icon icon-plus-sign\"></em>" +
          " Activate Stellar Bot</button><div id=\"postSuccess\" class=\"alert alert-block alert-success\">Your Stellar " +
          "bot has activated.</div></form>"
          $("#stellarLoc").html(html)
          $("#step2").children("h3").find(".icon").remove()
        )
      false

    $this.on "click", "#addBtn", ->
      $("#stellarLoc").find(".error").remove()
      $button = $(this)
      $button.find(".icon").addClass("fadeinout-enabled")
      # Ajax post
      name = $("#stellarNameInput").val()
      future = $.post("/stellar", { stellarName: name })
      future.done (data) ->
        $("#robotImg").addClass("robotmove-enabled")
        # Success message
        $("#addBtn").hide()
        $("#postSuccess").fadeIn(400).delay(4000).fadeOut(400, ->
          # Deactivate form
          html = "<div id=\"delete\"><h4>stellar.io/" + name + "</h4><a id=\"deleteBtn\" href=\"/\" class=\"btn btn-large btn-inverse btn-block\">" +
            "<em class=\"icon icon-off\"></em> Deactivate Stellar Bot</a><div id=\"deleteSuccess\" class=\"alert alert-block alert-success\">" +
            "Your Stellar bot has deactivated.</div></div>"
          $("#stellarLoc").html(html)
          $("#step2").children("h3").prepend("<em class=\"icon icon-ok\"></em> ")
        )
      future.fail -> $("#stellarLoc").prepend("<div class=\"error\">Oops! That isn't a valid Stellar user.</div>")
      future.always -> $button.find(".icon").removeClass("fadeinout-enabled")

      false