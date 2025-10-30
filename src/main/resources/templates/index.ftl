<!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <title>Enoch Leathercraft — ${title!''}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>body{font-family:system-ui,Arial;padding:24px;background:#111;color:#eee}</style>
</head>
<body>
<h1>${title!''}</h1>
<p>Nouvelle capsule. <a href="/produits" style="color:#e3ded3">Voir les produits</a></p>

<#if products?has_content>
    <h2>Quelques produits</h2>
    <ul>
        <#list products as p>
            <li>${p.name} — ${p.price} € (stock: ${p.stock})</li>
        </#list>
    </ul>
</#if>
</body>
</html>
