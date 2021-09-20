import { FoodType } from './FoodType';
import { FoodCategory } from './FoodCategory';

export class Food {

    public delete: boolean;

    public id: number;

    public code: string;

    public name: string;

    public foodType: FoodType;

    public price: number = 0;

    public defaultQuantity: number;

    public base64Image: string;

    public foodCategory: FoodCategory;

    public bigDescription: string;

    public active: boolean;

}
