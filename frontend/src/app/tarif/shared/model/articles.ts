export class Article {

    article: string;
    code: string;
    description: string;

    constructor(code: string, article: string, description: string){
        this.article = article;
        this.code = code;
        this.description = description;
    }

 }

export const articles : Article[] =
    [
        {
            article : "sel",
            code : "1",
            description : "Jonathan Littell",
        },
        {
            article : "livres",
            code : "2",
            description : "Amélie Nothomb",
        },
        {
            article : "voitures diesel",
            code : "3",
            description : "Nothomb",
        }
    ]
