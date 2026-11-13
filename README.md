# Hytale Shop

Shop administrateur pour serveur Hytale, développé par l'association étudiante
**HytaleAssos**.

Les joueurs achètent et vendent des blocs contre des **Hcoins**, la monnaie du
serveur, depuis une interface en jeu ou depuis le chat. Le shop est tenu par les
admins : stock et fonds illimités. Il propose automatiquement tous les blocs du
jeu (environ 2 600), avec un prix calculé que les admins peuvent changer.

## Fonctionnalités

- Monnaie **Hcoin** : solde par joueur, solde de départ configurable (0 par
  défaut, les joueurs gagnent leurs Hcoins en vendant), virements entre
  joueurs, classement des plus riches.
- Tous les blocs du jeu ajoutés automatiquement : roches, terres, bois,
  plantes, métal, tissu, déco, minerais et meubles.
- Interface `/shop` paginée (25 articles par page) avec onglets par catégorie et
  recherche ; une ligne par article avec icône, nom, prix d'achat, prix de
  revente, quantité possédée, et boutons **Buy**, **Sell**, **Sell all**.
- Commandes chat équivalentes : `/shop buy`, `/shop sell`, `/shop sellall`,
  `/shop list [page]`, `/shop search <texte>`.
- Commandes admin : `/eco give|take|set` pour les soldes, `/shopadmin
  set|remove|reset|reload` pour les prix et le catalogue.
- Transactions sûres : l'argent et les objets bougent dans un ordre qui ne crée
  jamais de valeur, avec remboursement si l'inventaire est plein.
- Persistance JSON (`accounts.json`, `shop.json`) avec écriture atomique,
  sauvegarde `.bak` et rollback mémoire si l'écriture échoue.

## Documentation

- [Commandes](docs/commandes.md) : toutes les commandes joueur et admin, avec
  leurs arguments, permissions et exemples.
- [Interface](docs/interface.md) : utilisation de la fenêtre du shop.

## Configuration

`shop_config.json`, dans le dossier de données du plugin, est créé au premier
démarrage :

| Clé | Défaut | Rôle |
| --- | --- | --- |
| `CurrencyName` | `Hcoin` | Nom de la monnaie affiché partout. |
| `StartingBalance` | `0` | Solde d'un joueur qui n'a jamais eu de compte. |
| `MaxTransactionQuantity` | `1000` | Quantité maximale par achat ou vente. |
| `TopListSize` | `10` | Nombre de lignes de `/hcointop`. |
| `IncludeAllBlocks` | `true` | Ajoute automatiquement tous les blocs du jeu. Si `false`, seuls les articles de `shop.json` sont proposés. |
| `SellPricePercent` | `25` | Prix de revente automatique, en % du prix d'achat (minimum 1 Hcoin, `0` = revente désactivée). |
| `RequirePermissions` | `false` | Si `true`, les commandes joueur exigent leur permission. |

Redémarrer le serveur après une modification.

## Prix automatiques

Un bloc est ajouté automatiquement s'il est plaçable, appartient à une
catégorie `Blocks.*` (sauf fluides et portails) ou `Furniture.*`, et n'est pas
un objet de développement (qualités Developer, Technical, Template, Tool,
Debug).

Prix d'achat = prix de base de la catégorie, doublé à chaque rang de qualité
au-dessus de Common (Uncommon x2, Rare x4, Epic x8, Legendary x16) :

| Catégorie | Prix de base |
| --- | --- |
| Soils | 10 |
| Rocks, Plants | 15 |
| Wood | 20 |
| Cloth | 25 |
| Deco | 40 |
| Metal | 75 |
| Ores | 100 |
| Furniture | 125 |

Prix de revente = `SellPricePercent` % du prix d'achat, au moins 1.

Un admin change n'importe quel prix avec `/shopadmin set`, retire un bloc avec
`/shopadmin remove` et annule ces changements avec `/shopadmin reset`.

## Permissions

Les commandes joueur sont ouvertes à tous tant que `RequirePermissions` vaut
`false`. Les commandes admin exigent **toujours** `hytale.shop.admin`.

| Permission | Commandes |
| --- | --- |
| `hytale.shop.shop` | `/shop` et ses sous-commandes |
| `hytale.shop.balance` | `/hcoin` |
| `hytale.shop.balance.others` | `/hcoin <joueur>` |
| `hytale.shop.pay` | `/pay` |
| `hytale.shop.top` | `/hcointop` |
| `hytale.shop.admin` | `/eco`, `/shopadmin` |

## Données

Fichiers dans le dossier de données du plugin
(`mods/HytaleAssos_HytaleShop/` sur le serveur) :

- `accounts.json` : soldes par UUID de joueur, avec le dernier pseudo connu.
- `shop.json` : uniquement les changements des admins. Les blocs automatiques
  n'y sont pas écrits.

```json
{
  "version": 2,
  "items": [
    { "itemId": "Rock_Stone", "buyPrice": 25, "sellPrice": 6 }
  ],
  "excluded": [ "Soil_Gravel" ]
}
```

- `items` : prix personnalisés. Ils remplacent le prix automatique d'un bloc,
  ou ajoutent un objet qui n'est pas un bloc (lingot, nourriture...).
- `excluded` : blocs automatiques retirés du shop.

- `buyPrice` : Hcoins payés par le joueur pour une unité. `0` = le shop ne le
  vend pas.
- `sellPrice` : Hcoins reçus par le joueur pour une unité. `0` = le shop ne le
  rachète pas.
- Les entrées invalides (id vide, prix négatif, deux prix à `0`) sont ignorées
  au chargement, avec un avertissement dans les logs. Les articles dont l'id
  n'existe pas dans le jeu sont masqués de l'interface et de `/shop list`.

Les articles sont affichés triés par id. `shop.json` peut être modifié à la
main puis rechargé avec `/shopadmin reload`. Un fichier `version: 1` (ancien
format) est lu tel quel : ses articles deviennent des prix personnalisés.

Si un fichier est illisible au démarrage, le plugin refuse toute écriture
dessus pour ne pas l'écraser : les soldes ne peuvent plus changer (ou le
catalogue ne peut plus être modifié) tant que le fichier n'est pas réparé. Pour
le catalogue, `/shopadmin reload` suffit après réparation ; pour les comptes,
il faut redémarrer.

## Limites connues

- `/pay`, `/eco` et `/hcoin <joueur>` ne visent que des joueurs connectés.
- L'inventaire pris en compte est la barre rapide, le stockage et le sac à dos,
  pas l'armure ni les emplacements utilitaires.
- Un admin peut fixer un prix de revente supérieur au prix d'achat ;
  `/shopadmin set` l'avertit car cela permet de générer des Hcoins à l'infini.
- La recherche porte sur l'id de l'objet (`stone`, `oak plank`), pas sur son
  nom traduit : le serveur ne connaît pas les traductions.
- Les prix automatiques sont une base : les blocs rares d'une même catégorie
  coûtent pareil tant qu'un admin ne les ajuste pas.
- L'interface et les commandes n'ont pas de test automatisé : elles se
  vérifient en jeu avec `./gradlew runServer`.

## Construction et tests

```bash
./gradlew build     # compile, teste et produit build/libs/Hytale Shop-<version>.jar
./gradlew test      # tests unitaires seulement
./gradlew runServer # serveur de développement local
```

Les tests unitaires (`src/test/java`) couvrent `EconomyService`,
`AccountStorage`, `ShopCatalog`, `ShopEntry`, `ShopService`, `ShopFilter`,
`ShopCategory` et `AutoPricing`, avec un faux inventaire. Ils n'utilisent que des dossiers temporaires.

## Structure

```
src/main/java/assos/hytale/shop/
  HytaleShopPlugin.java   point d'entrée, enregistrement des commandes
  ShopMessages.java       textes et couleurs des messages
  ShopPermissions.java    noeuds de permission
  config/ShopConfig.java  configuration (shop_config.json)
  economy/                comptes Hcoin et leur stockage
  shop/                   catalogue, blocs du jeu, prix automatiques, filtres,
                          règles d'achat/vente, accès à l'inventaire
  commands/               commandes chat
  ui/ShopPage.java        fenêtre du shop
src/main/resources/Common/UI/Custom/Pages/HytaleShop/
  ShopPage.ui             mise en page de la fenêtre
  ShopItemRow.ui          ligne d'article
```

## Compatibilité

- Java 25, serveur Hytale `>=0.7.2 <0.8.0` (compilé contre l'API 0.7.7).
- Le plugin embarque un asset pack (`IncludesAssetPack: true`) pour ses fichiers
  d'interface, et dépend de `Hytale:AssetModule`.
