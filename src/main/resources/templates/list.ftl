<!doctype html>
<html lang="fr">
<head><meta charset="utf-8"><title>Produits</title></head>
<body>
<h1>Produits</h1>
<#if products?has_content>
    <table border="1" cellpadding="6">
        <tr><th>ID</th><th>Nom</th><th>Prix</th><th>Stock</th></tr>
        <#list products as p>
            <tr>
                <td>${p.id}</td><td>${p.name}</td><td>${p.price}</td><td>${p.stock}</td>
            </tr>
        </#list>
    </table>
<#else>
    <p>Aucun produit.</p>
</#if>
</body>
</html>
