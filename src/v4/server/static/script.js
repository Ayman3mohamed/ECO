var log_div, template, nextMsgRight, emoji;

window.onload = function () {
    log_div = $('#log')
    template = $('#msgTemplate')
    nextMsgRight = false
    emoji = new EmojiConvertor();
 }



function appendMsg(socketMsg) {
    var text = emoji.replace_colons(socketMsg.text);
    var user = socketMsg.user;
    var attachment = socketMsg.attachment;
    var style = socketMsg.style;
    var timestamp = socketMsg.timestamp;

    var msgObj = template.clone()
    msgObj.removeAttr('id')

    msgObj.find('.timeStamp').text(user + ": " + timestamp)

    // checking whether the text should be added to a
    // <pre> (pre-formatted) or <div> tag
    // important for pre-formatted text like ascii stuff
    // TODO style is supposed to be a more complex object, but for now... ok
    // TODO let's also have a default style use its string representation on the top of the file.
    // this is magic number style and often leads to problems when another developer cannot read your mind
    if(style == "unformatted") {
        msgObj.find('.msgTextDiv').text(text)
    } else if (style == "formatted") {
        msgObj.find('.msgTextPre').text(text)
    }

    if( attachment == null) {
        // remove the image tag
        msgObj.find('.image').remove()
    } else {
        // append the image and remove the text
        msgObj.find('.image').attr( 'src', attachment );
        msgObj.find('.msgTextDiv').remove()
        msgObj.find('.msgTextPre').remove()
    }

    // adds some css class to divs for left and right style
    msgObj.find('.msgBox').addClass(nextMsgRight ? 'right' : 'left')

    // adds the customized msg to the log
    log_div.append(msgObj)

    nextMsgRight = !nextMsgRight
}

