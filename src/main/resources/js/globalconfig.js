AJS.toInit(function () {
    var $pageMetadata = AJS.$('#content.page.view .page-metadata:first');

    if ($pageMetadata.length > 0) {
        var selectedAjsParams = {
            config: AJS.params.config,
            errors: AJS.params.errors
        }
        var template = com.atlassian.stash.repository.hook.ref.formContents(selectedAjsParams);
        $pageMetadata.after(template);
    }
});